package org.config.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public record ConfigDto(
        @NotBlank(message = "Jméno konfigurace nesmí být prázdné")
        String name,

        String description,

        @Min(value = 0, message = "Timeout nesmí být záporný")
        Integer timeout,

        String userAgent,

        String url,

        String customText,

        /** Text webové stránky - buď dodaný klientem (už naskrapovaný), nebo (pokud chybí) doplněný automaticky z {@code url}. */
        String webText,

        /**
         * Tabulky z webové stránky ve stejném formátu, jaký vrací ScrapperService pro strategii
         * EXTRACT_TABLES (pole "tables" - seznam {@code {"table": {"columns": [...], "row": [...]}}}).
         */
        List<Map<String, Object>> tables,

        List<FileDto> files
) {}
