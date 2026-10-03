package org.config.service.webfile;

public interface WebFileDownloadService {

    /**
     * Stáhne obsah souboru z libovolné webové URL a nahraje ho do Supabase Storage na zadanou
     * cestu. Běží asynchronně na pozadí a je čistě best-effort - jakékoli selhání (stažení i
     * upload) se jen zaloguje, nikdy nesmí ovlivnit request, který config vytvořil (stejný princip
     * jako {@link org.config.service.storage.SupabaseStorageService#uploadFileAsync}).
     */
    void downloadAndStoreAsync(String storagePath, String sourceUrl, String fileType);
}
