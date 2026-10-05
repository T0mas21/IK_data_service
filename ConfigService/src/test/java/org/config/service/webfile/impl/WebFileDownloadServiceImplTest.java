package org.config.service.webfile.impl;

import org.config.service.storage.SupabaseStorageService;
import org.config.service.webfile.UrlContent;
import org.config.service.webfile.UrlContentFetcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebFileDownloadServiceImplTest {

    @Mock
    private UrlContentFetcher urlContentFetcher;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    @InjectMocks
    private WebFileDownloadServiceImpl service;

    @Test
    void downloadAndStoreAsync_IsAnnotatedAsync() throws NoSuchMethodException {
        Method method = WebFileDownloadServiceImpl.class.getMethod(
                "downloadAndStoreAsync", String.class, String.class, String.class);

        assertTrue(method.isAnnotationPresent(Async.class));
    }

    @Test
    void downloadAndStoreAsync_UploadsDownloadedBytes_WhenDownloadSucceeds() {
        byte[] downloaded = "obsah souboru".getBytes();
        when(urlContentFetcher.fetch("https://example.com/priloha.pdf"))
                .thenReturn(new UrlContent(downloaded, "application/octet-stream"));

        service.downloadAndStoreAsync("configs/1/uuid_priloha.pdf", "https://example.com/priloha.pdf", "application/pdf");

        verify(supabaseStorageService).uploadFile("configs/1/uuid_priloha.pdf", downloaded, "application/pdf");
    }

    @Test
    void downloadAndStoreAsync_PouzijeContentTypeZeStazeneOdpovedi_KdyzKlientFileTypeNeposlal() {
        byte[] downloaded = "obsah souboru".getBytes();
        when(urlContentFetcher.fetch("https://example.com/priloha.pdf"))
                .thenReturn(new UrlContent(downloaded, "application/pdf"));

        service.downloadAndStoreAsync("configs/1/uuid_priloha.pdf", "https://example.com/priloha.pdf", null);

        verify(supabaseStorageService).uploadFile("configs/1/uuid_priloha.pdf", downloaded, "application/pdf");
    }

    @Test
    void downloadAndStoreAsync_PouzijeContentTypeZeStazeneOdpovedi_KdyzKlientFileTypePoslalPrazdny() {
        byte[] downloaded = "obsah souboru".getBytes();
        when(urlContentFetcher.fetch("https://example.com/priloha.pdf"))
                .thenReturn(new UrlContent(downloaded, "application/pdf"));

        service.downloadAndStoreAsync("configs/1/uuid_priloha.pdf", "https://example.com/priloha.pdf", "   ");

        verify(supabaseStorageService).uploadFile("configs/1/uuid_priloha.pdf", downloaded, "application/pdf");
    }

    @Test
    void downloadAndStoreAsync_DoesNotThrow_AndNeverUploads_WhenDownloadFails() {
        when(urlContentFetcher.fetch(anyString())).thenThrow(new RuntimeException("timeout"));

        assertDoesNotThrow(() -> service.downloadAndStoreAsync(
                "configs/1/uuid_priloha.pdf", "https://example.com/priloha.pdf", "application/pdf"));

        verify(supabaseStorageService, never()).uploadFile(anyString(), any(), anyString());
    }

    @Test
    void downloadAndStoreAsync_DoesNotThrow_WhenUploadFails() {
        when(urlContentFetcher.fetch(anyString())).thenReturn(new UrlContent("obsah".getBytes(), "application/pdf"));
        when(supabaseStorageService.uploadFile(anyString(), any(), anyString()))
                .thenThrow(new RuntimeException("Supabase je nedostupné"));

        assertDoesNotThrow(() -> service.downloadAndStoreAsync(
                "configs/1/uuid_priloha.pdf", "https://example.com/priloha.pdf", "application/pdf"));
    }
}
