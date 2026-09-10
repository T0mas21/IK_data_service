package org.vector.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SearchRequestDto(
        Long configId,
        @NotBlank(message = "Dotaz nesmí být prázdný")
        String query,
        @Min(value = 1, message = "topK musí být alespoň 1")
        Integer topK
) {}
