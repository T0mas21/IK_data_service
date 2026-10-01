package org.vector.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.vector.dto.FileRefDto;
import org.vector.dto.IndexRequestDto;
import org.vector.enums.SourceType;
import org.vector.service.IndexingService;
import org.vector.service.extraction.TextExtractionService;
import org.vector.service.storage.SupabaseFileClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Service
public class IndexingServiceImpl implements IndexingService {

    private static final Logger log = LoggerFactory.getLogger(IndexingServiceImpl.class);

    private static final int MAX_SEGMENT_SIZE_IN_CHARS = 500;
    private static final int MAX_OVERLAP_SIZE_IN_CHARS = 50;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final SupabaseFileClient supabaseFileClient;
    private final TextExtractionService textExtractionService;
    private final DocumentSplitter documentSplitter =
            DocumentSplitters.recursive(MAX_SEGMENT_SIZE_IN_CHARS, MAX_OVERLAP_SIZE_IN_CHARS);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IndexingServiceImpl(EmbeddingModel embeddingModel,
                                EmbeddingStore<TextSegment> embeddingStore,
                                SupabaseFileClient supabaseFileClient,
                                TextExtractionService textExtractionService) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.supabaseFileClient = supabaseFileClient;
        this.textExtractionService = textExtractionService;
    }

    @Override
    public int indexConfig(IndexRequestDto request) {
        Long configId = request.configId();

        embeddingStore.removeAll(metadataKey("configId").isEqualTo(configId));

        List<TextSegment> segments = new ArrayList<>();

        if (isNotBlank(request.customText())) {
            segments.addAll(splitWithMetadata(request.customText(), baseMetadata(configId, SourceType.CUSTOM_TEXT, null, null)));
        }

        if (isNotBlank(request.scrapedText())) {
            segments.addAll(splitWithMetadata(request.scrapedText(), baseMetadata(configId, SourceType.SCRAPED, null, null)));
        }

        if (isNotBlank(request.tables())) {
            for (String tableText : tablesToText(request.tables())) {
                segments.addAll(splitWithMetadata(tableText, baseMetadata(configId, SourceType.TABLE, null, null)));
            }
        }

        if (request.files() != null) {
            for (FileRefDto file : request.files()) {
                String text = extractFileTextSafely(file);
                if (isNotBlank(text)) {
                    segments.addAll(splitWithMetadata(text, baseMetadata(configId, SourceType.FILE, file.fileId(), file.fileName())));
                }
            }
        }

        if (segments.isEmpty()) {
            return 0;
        }

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);

        return segments.size();
    }

    @Override
    public void deleteConfig(Long configId) {
        embeddingStore.removeAll(metadataKey("configId").isEqualTo(configId));
    }

    /**
     * Převede syrový JSON tabulek (formát ScrapperService, pole "tables" - seznam
     * {@code {"table": {"columns": [...], "row": [...]}}}) na čitelný text, jeden řádek tabulky
     * na jeden výsledný text ("Sloupec1: hodnota1, Sloupec2: hodnota2, ..."), aby šel smysluplně
     * chunkovat a embedovat. Chybný/needitovatelný JSON se jen přeskočí (best-effort, viz
     * {@link #indexConfig}) - nesmí shodit celou indexaci.
     */
    @SuppressWarnings("unchecked")
    private List<String> tablesToText(String tablesJson) {
        List<Map<String, Object>> tables;
        try {
            tables = objectMapper.readValue(tablesJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("Nepodařilo se zpracovat tabulky z webu (neplatný JSON): {}", e.getMessage());
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (Map<String, Object> tableEntry : tables) {
            Object tableObj = tableEntry.get("table");
            if (!(tableObj instanceof Map)) {
                continue;
            }
            Map<String, Object> table = (Map<String, Object>) tableObj;
            Object rowsObj = table.get("row");
            if (!(rowsObj instanceof List)) {
                continue;
            }

            for (Object rowObj : (List<Object>) rowsObj) {
                if (!(rowObj instanceof Map)) {
                    continue;
                }
                String rowText = rowToText((Map<String, Object>) rowObj);
                if (!rowText.isBlank()) {
                    result.add(rowText);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private String rowToText(Map<String, Object> row) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> cell : row.entrySet()) {
            String value = cellValueToText(cell.getValue());
            if (value.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(cell.getKey()).append(": ").append(value);
        }
        return sb.toString();
    }

    /**
     * Hodnota buňky je buď prostý text, nebo (u buňky s odkazem) seznam s jedním objektem
     * {@code {"name": ..., "url": ...}} - viz ScrapperServiceImpl.getTable.
     */
    @SuppressWarnings("unchecked")
    private String cellValueToText(Object value) {
        if (value instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
            Map<String, Object> link = (Map<String, Object>) list.get(0);
            Object name = link.get("name");
            Object url = link.get("url");
            if (name != null && url != null) {
                return name + " (" + url + ")";
            }
            return String.valueOf(name != null ? name : url);
        }
        return value != null ? value.toString() : "";
    }

    private String extractFileTextSafely(FileRefDto file) {
        try {
            byte[] bytes = supabaseFileClient.downloadFile(file.storagePath());
            return textExtractionService.extractText(bytes);
        } catch (Exception e) {
            log.warn("Nepodařilo se zaindexovat soubor {} (configId={}): {}", file.fileName(), file.fileId(), e.getMessage());
            return null;
        }
    }

    private List<TextSegment> splitWithMetadata(String text, Metadata metadata) {
        Document document = Document.from(text, metadata);
        return documentSplitter.split(document);
    }

    private Metadata baseMetadata(Long configId, SourceType sourceType, Long fileId, String fileName) {
        Metadata metadata = new Metadata()
                .put("configId", configId)
                .put("sourceType", sourceType.name());
        if (fileId != null) {
            metadata.put("fileId", fileId);
        }
        if (fileName != null) {
            metadata.put("fileName", fileName);
        }
        return metadata;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
