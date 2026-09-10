package org.config.dto;

/**
 * Odpověď na vytvoření "signed upload URL" — klient na {@code uploadUrl} udělá PUT s obsahem
 * souboru přímo do Supabase Storage (bajty souboru tedy neprochází přes tuto aplikaci) a poté
 * zavolá registrační endpoint s {@code storagePath} z této odpovědi.
 */
public record UploadUrlDto(
        String storagePath,
        String uploadUrl,
        String fileName,
        String fileType
) {}
