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
        String content,

        /**
         * URL, ze které si server sám stáhne obsah souboru (typicky odkaz na soubor nalezený
         * scraperem na webové stránce - klient posílá jen název a odkaz, ne obsah). Platí jen při
         * vytváření configu ({@code createConfig}) - stažení i upload do Supabase Storage probíhá
         * asynchronně na pozadí, best-effort (viz {@code WebFileDownloadService}).
         */
        String sourceUrl
) {}
