package org.vector.dto;

public record SearchResultDto(
        String text,
        double score,
        String sourceType,
        Long fileId,
        String fileName
) {}
