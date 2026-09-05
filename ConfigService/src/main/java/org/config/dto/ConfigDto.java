package org.config.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ConfigDto(
        @NotBlank(message = "Jméno konfigurace nesmí být prázdné")
        String name,

        String description,

        @Min(value = 0, message = "Timeout nesmí být záporný")
        Integer timeout,

        String userAgent,

        String url,

        List<FileDto> files
) {}
