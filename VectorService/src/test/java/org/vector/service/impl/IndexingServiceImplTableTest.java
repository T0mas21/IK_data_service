package org.vector.service.impl;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vector.dto.IndexRequestDto;
import org.vector.enums.SourceType;
import org.vector.service.extraction.TextExtractionService;
import org.vector.service.storage.SupabaseFileClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexingServiceImplTableTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private SupabaseFileClient supabaseFileClient;

    @Mock
    private TextExtractionService textExtractionService;

    @InjectMocks
    private IndexingServiceImpl indexingService;

    @Test
    @SuppressWarnings("unchecked")
    void indexConfig_IndexesTables_AsReadableText() {
        String tablesJson = "[{\"table\":{\"columns\":[{\"1\":\"Název\"},{\"2\":\"Datum\"}],"
                + "\"row\":[{\"Název\":\"Žádost\",\"Datum\":\"1.1.2026\"}]}}]";
        IndexRequestDto request = new IndexRequestDto(1L, null, null, tablesJson, null);
        when(embeddingModel.embedAll(anyList())).thenReturn(Response.from(List.of()));

        indexingService.indexConfig(request);

        ArgumentCaptor<List<TextSegment>> captor = ArgumentCaptor.forClass(List.class);
        verify(embeddingModel).embedAll(captor.capture());
        List<TextSegment> segments = captor.getValue();

        assertFalse(segments.isEmpty());
        TextSegment segment = segments.get(0);
        assertEquals(SourceType.TABLE.name(), segment.metadata().getString("sourceType"));
        assertTrue(segment.text().contains("Název: Žádost"));
        assertTrue(segment.text().contains("Datum: 1.1.2026"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void indexConfig_SkipsTables_WhenTablesIsBlank() {
        IndexRequestDto request = new IndexRequestDto(1L, "nejaky text", null, null, null);
        when(embeddingModel.embedAll(anyList())).thenReturn(Response.from(List.of()));

        indexingService.indexConfig(request);

        ArgumentCaptor<List<TextSegment>> captor = ArgumentCaptor.forClass(List.class);
        verify(embeddingModel).embedAll(captor.capture());
        boolean hasTableSegment = captor.getValue().stream()
                .anyMatch(s -> SourceType.TABLE.name().equals(s.metadata().getString("sourceType")));

        assertFalse(hasTableSegment);
    }

    @Test
    void indexConfig_DoesNotThrow_WhenTablesJsonIsMalformed() {
        IndexRequestDto request = new IndexRequestDto(1L, "nejaky text", null, "not-valid-json", null);
        when(embeddingModel.embedAll(anyList())).thenReturn(Response.from(List.of()));

        assertDoesNotThrow(() -> indexingService.indexConfig(request));
    }
}
