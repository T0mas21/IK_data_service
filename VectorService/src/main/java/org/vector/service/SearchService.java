package org.vector.service;

import org.vector.dto.SearchRequestDto;
import org.vector.dto.SearchResultDto;

import java.util.List;

public interface SearchService {

    List<SearchResultDto> search(SearchRequestDto request);
}
