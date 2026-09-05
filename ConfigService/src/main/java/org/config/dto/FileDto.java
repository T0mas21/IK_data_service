package org.config.dto;

public record FileDto(
        Long id,
        String fileName,
        String storagePath,
        String fileType
) {}
