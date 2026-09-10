package org.vector.service;

import org.vector.dto.IndexRequestDto;

public interface IndexingService {

    int indexConfig(IndexRequestDto request);

    void deleteConfig(Long configId);
}
