package org.config.dto;

public record FileDto(
        Long id,
        String fileName,
        String storagePath,
        String fileType,

        /**
         * Base64 obsah souboru. Vyplněný = nový/nahrazovaný obsah k nahrání při editaci configu.
         * Prázdný/null = jen reference na již existující soubor (identifikovaný přes fileName).
         */
        String content
) {}
