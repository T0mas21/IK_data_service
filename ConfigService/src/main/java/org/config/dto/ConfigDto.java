package org.config.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfigDto(
        @NotBlank(message = "Jméno konfigurace nesmí být prázdné")
        String name,

        String data
)
{}
