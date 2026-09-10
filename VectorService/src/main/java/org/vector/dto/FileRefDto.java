package org.vector.dto;

import jakarta.validation.constraints.NotBlank;

public record FileRefDto(
        Long fileId,
        @NotBlank(message = "Jméno souboru nesmí být prázdné")
        String fileName,
        @NotBlank(message = "Cesta souboru v úložišti nesmí být prázdná")
        String storagePath,
        String fileType
) {}
