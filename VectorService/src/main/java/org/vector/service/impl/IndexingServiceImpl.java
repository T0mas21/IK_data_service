package org.vector.service.impl;

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
