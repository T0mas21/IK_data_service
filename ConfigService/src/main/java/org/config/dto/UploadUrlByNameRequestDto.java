package org.config.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Vyžádání signed upload URL ještě předtím, než config v DB existuje. Cesta v úložišti se
 * odvozuje z {@code configName} (jediné pole, které klient zná dopředu, je unikátní a po
 * vytvoření configu se už nemění) místo z {@code configId}.
 */
public record UploadUrlByNameRequestDto(
        @NotBlank(message = "Jméno konfigurace nesmí být prázdné")
        String configName,

        @NotBlank(message = "Jméno souboru nesmí být prázdné")
        String fileName,

        String fileType
) {}
