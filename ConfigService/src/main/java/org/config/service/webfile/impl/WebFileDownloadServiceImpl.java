package org.config.service.webfile.impl;

import org.config.service.storage.SupabaseStorageService;
import org.config.service.webfile.UrlContent;
import org.config.service.webfile.UrlContentFetcher;
import org.config.service.webfile.WebFileDownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class WebFileDownloadServiceImpl implements WebFileDownloadService {

    private static final Logger log = LoggerFactory.getLogger(WebFileDownloadServiceImpl.class);

    private final UrlContentFetcher urlContentFetcher;
    private final SupabaseStorageService supabaseStorageService;

    public WebFileDownloadServiceImpl(UrlContentFetcher urlContentFetcher, SupabaseStorageService supabaseStorageService) {
        this.urlContentFetcher = urlContentFetcher;
        this.supabaseStorageService = supabaseStorageService;
    }

    @Async
    @Override
    public void downloadAndStoreAsync(String storagePath, String sourceUrl, String fileType) {
        try {
            UrlContent urlContent = urlContentFetcher.fetch(sourceUrl);
            // klient typ souboru u odkazů z webu typicky nezná - pokud ho nepošle, použij
            // Content-Type, který se stáhl spolu s obsahem (jinak by Supabase Storage upload
            // skončil na application/octet-stream, který bucket odmítá)
            String effectiveFileType = fileType != null && !fileType.isBlank() ? fileType : urlContent.contentType();
            supabaseStorageService.uploadFile(storagePath, urlContent.content(), effectiveFileType);
        } catch (Exception e) {
            log.warn("Stažení souboru z webu ({}) a uložení do Supabase Storage selhalo: {}", sourceUrl, e.getMessage());
        }
    }
}
