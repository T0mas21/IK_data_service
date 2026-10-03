package org.config.service.storage;

public interface SupabaseStorageService {

    String uploadFile(String storagePath, byte[] content, String contentType);

    /**
     * Stejné jako {@link #uploadFile}, ale běží asynchronně na pozadí a chyby jen loguje.
     * Render free tier má vlastní gateway timeout kratší než náš connect/read timeout na
     * {@code RestTemplate} - synchronní upload uvnitř HTTP requestu proto riskuje 502 dřív,
     * než by stihl selhat náš vlastní timeout. Volající by měl storagePath uložit hned
     * (viz {@link #uploadFile}), tahle metoda jen dodá skutečný obsah do Supabase Storage.
     */
    void uploadFileAsync(String storagePath, byte[] content, String contentType);

    /**
     * Stáhne obsah souboru ze Supabase Storage. Používá se při čtení configu, kdy se obsah souborů
     * vrací klientovi jako base64 - pro pouhé předání odkazu klientovi je levnější
     * {@link #createSignedDownloadUrl} (bajty pak aplikací vůbec neprochází).
     */
    byte[] downloadFile(String storagePath);

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
