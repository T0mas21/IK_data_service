package org.config.service.storage;

public interface SupabaseStorageService {

    String uploadFile(String storagePath, byte[] content, String contentType);

    void deleteFile(String storagePath);

    /**
     * Vytvoří jednorázovou "signed upload URL", na kterou klient může nahrát obsah souboru
     * přímo do Supabase Storage, aniž by bajty procházely přes tuto aplikaci.
     *
     * @return plná URL (včetně tokenu), na kterou se dělá PUT s obsahem souboru
     */
    String createSignedUploadUrl(String storagePath);

    /**
     * Vytvoří jednorázovou "signed download URL" s omezenou platností, na kterou lze přesměrovat
     * klienta, aniž by bajty souboru procházely přes tuto aplikaci.
     */
    String createSignedDownloadUrl(String storagePath);
}
