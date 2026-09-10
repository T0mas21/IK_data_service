package org.vector.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record IndexRequestDto(
        @NotNull(message = "configId nesmí být prázdné")
        Long configId,
        String customText,
        String scrapedText,
        @Valid
        List<FileRefDto> files
) {}
