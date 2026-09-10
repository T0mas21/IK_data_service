package org.config.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Zaregistrování souboru, který už klient nahrál přímo do Supabase Storage přes signed upload URL
 * (viz {@link UploadUrlDto}) — {@code storagePath} musí odpovídat cestě z předchozí odpovědi.
 */
public record RegisterFileDto(
        @NotBlank(message = "storagePath nesmí být prázdný")
        String storagePath,
        @NotBlank(message = "Jméno souboru nesmí být prázdné")
        String fileName,
        String fileType
) {}
