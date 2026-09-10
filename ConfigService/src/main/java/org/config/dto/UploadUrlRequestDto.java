package org.config.dto;

import jakarta.validation.constraints.NotBlank;

public record UploadUrlRequestDto(
        @NotBlank(message = "Jméno souboru nesmí být prázdné")
        String fileName,
        String fileType
) {}
