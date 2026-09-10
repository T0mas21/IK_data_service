package org.vector.service.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;
import org.vector.dto.SearchRequestDto;
import org.vector.dto.SearchResultDto;
import org.vector.service.SearchService;

import java.util.List;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Service
public class SearchServiceImpl implements SearchService {

    private static final int DEFAULT_TOP_K = 5;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public SearchServiceImpl(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @Override
    public List<SearchResultDto> search(SearchRequestDto request) {
        Embedding queryEmbedding = embeddingModel.embed(request.query()).content();
        int maxResults = request.topK() != null ? request.topK() : DEFAULT_TOP_K;

        var requestBuilder = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults);

        if (request.configId() != null) {
            requestBuilder = requestBuilder.filter(metadataKey("configId").isEqualTo(request.configId()));
        }

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(requestBuilder.build());

        return result.matches().stream()
                .map(this::toSearchResultDto)
                .toList();
    }

    private SearchResultDto toSearchResultDto(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();
        return new SearchResultDto(
                segment.text(),
                match.score(),
                segment.metadata().getString("sourceType"),
                segment.metadata().getLong("fileId"),
                segment.metadata().getString("fileName")
        );
    }
}
