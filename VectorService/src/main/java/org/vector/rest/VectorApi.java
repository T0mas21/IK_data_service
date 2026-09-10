package org.vector.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vector.dto.IndexRequestDto;
import org.vector.dto.IndexResponseDto;
import org.vector.dto.SearchRequestDto;
import org.vector.dto.SearchResponseDto;
import org.vector.service.IndexingService;
import org.vector.service.SearchService;

@RestController
@RequestMapping("/scrapper_api/vector")
public class VectorApi {

    private final IndexingService indexingService;
    private final SearchService searchService;

    public VectorApi(IndexingService indexingService, SearchService searchService) {
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @PostMapping("/index")
    public ResponseEntity<IndexResponseDto> index(@Valid @RequestBody IndexRequestDto request) {
        int chunksIndexed = indexingService.indexConfig(request);
        return ResponseEntity.ok(new IndexResponseDto(chunksIndexed));
    }

    @DeleteMapping("/configs/{configId}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long configId) {
        indexingService.deleteConfig(configId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    public ResponseEntity<SearchResponseDto> search(@Valid @RequestBody SearchRequestDto request) {
        SearchResponseDto response = new SearchResponseDto(searchService.search(request));
        return ResponseEntity.ok(response);
    }
}
