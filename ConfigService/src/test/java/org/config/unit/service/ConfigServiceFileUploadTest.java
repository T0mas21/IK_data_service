package org.config.unit.service;

import org.config.data.model.Config;
import org.config.data.model.File;
import org.config.dto.UploadUrlDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConfigServiceFileUploadTest extends BaseConfigServiceTest {

    @Test
    void addFileToConfig_Success_UploadsToSupabaseAndReindexes() {
        Config config = new Config();
        config.setId(1L);
        config.setUrl("https://example.com");
        config.setUserAgent("Mozilla/5.0");
        config.setTimeout(10);
        config.setCustomText("nejaky text");

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "smlouva.pdf", "application/pdf", "obsah souboru".getBytes());

        when(configRepository.findByIdWithFiles(1L)).thenReturn(Optional.of(config));
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(supabaseStorageService.uploadFile(anyString(), any(byte[].class), eq("application/pdf")))
                .thenReturn("configs/1/uuid_smlouva.pdf");
        when(scrapperServiceClient.scrapeText(anyString(), any(), any())).thenReturn("naskrapovany text");

        File result = configService.addFileToConfig(1L, multipartFile);

        assertNotNull(result);
        assertEquals("smlouva.pdf", result.getFileName());
        assertEquals("application/pdf", result.getFileType());
        assertTrue(result.getStoragePath().startsWith("configs/1/"));
        assertTrue(result.getStoragePath().endsWith("_smlouva.pdf"));

        // soubor se opravdu nahraje do Supabase Storage (bajty projdou přes appku)
        verify(supabaseStorageService, times(1))
                .uploadFile(anyString(), any(byte[].class), eq("application/pdf"));
        verify(fileRepository, times(1)).save(any(File.class));

        // config se po přidání souboru přeindexuje (scrape + vector index)
        verify(scrapperServiceClient, times(1))
                .scrapeText("https://example.com", 10, "Mozilla/5.0");
        verify(vectorServiceClient, times(1))
                .indexConfig(eq(1L), eq("nejaky text"), eq("naskrapovany text"), anyList());
    }

    @Test
    void addFileToConfig_ThrowsException_WhenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "prazdny.txt", "text/plain", new byte[0]);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.addFileToConfig(1L, emptyFile)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(supabaseStorageService, never()).uploadFile(anyString(), any(), anyString());
        verify(fileRepository, never()).save(any());
    }

    @Test
    void addFileToConfig_ThrowsException_WhenConfigNotFound() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "obsah".getBytes());
        when(configRepository.findByIdWithFiles(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.addFileToConfig(99L, file)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(supabaseStorageService, never()).uploadFile(anyString(), any(), anyString());
    }

    @Test
    void createUploadUrl_Success_DoesNotPersistFileYet() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(new Config()));
        when(supabaseStorageService.createSignedUploadUrl(anyString()))
                .thenReturn("https://supabase.example/storage/v1/object/upload/sign/config-files/configs/1/uuid_smlouva.pdf?token=abc");

        UploadUrlDto result = configService.createUploadUrl(1L, "smlouva.pdf", "application/pdf");

        assertNotNull(result);
        assertTrue(result.storagePath().startsWith("configs/1/"));
        assertTrue(result.storagePath().endsWith("_smlouva.pdf"));
        assertEquals("smlouva.pdf", result.fileName());
        assertEquals("application/pdf", result.fileType());
        assertTrue(result.uploadUrl().contains("token="));

        // v tomto kroku se ještě nic neukládá do DB ani nenahrávají bajty
        verify(fileRepository, never()).save(any());
        verify(supabaseStorageService, never()).uploadFile(anyString(), any(), anyString());
        verify(vectorServiceClient, never()).indexConfig(any(), any(), any(), any());
    }

    @Test
    void createUploadUrl_ThrowsException_WhenConfigNotFound() {
        when(configRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.createUploadUrl(99L, "a.pdf", "application/pdf")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(supabaseStorageService, never()).createSignedUploadUrl(anyString());
    }

    @Test
    void createUploadUrlForNewConfig_Success_BuildsPathFromSanitizedName() {
        when(supabaseStorageService.createSignedUploadUrl(anyString()))
                .thenReturn("https://supabase.example/storage/v1/object/upload/sign/config-files/configs/muj_config/uuid_smlouva.pdf?token=abc");

        UploadUrlDto result = configService.createUploadUrlForNewConfig("Muj Config!", "smlouva.pdf", "application/pdf");

        assertNotNull(result);
        assertTrue(result.storagePath().startsWith("configs/muj_config/"));
        assertTrue(result.storagePath().endsWith("_smlouva.pdf"));
        assertEquals("smlouva.pdf", result.fileName());
        assertEquals("application/pdf", result.fileType());

        // config v tuto chvíli v DB vůbec nemusí existovat
        verify(configRepository, never()).findById(anyLong());
        verify(configRepository, never()).findByName(anyString());
    }

    @Test
    void createUploadUrlForNewConfig_ThrowsException_WhenConfigNameIsBlank() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.createUploadUrlForNewConfig("  ", "smlouva.pdf", "application/pdf")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(supabaseStorageService, never()).createSignedUploadUrl(anyString());
    }

    @Test
    void createUploadUrlForNewConfig_ThrowsException_WhenFileNameIsBlank() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.createUploadUrlForNewConfig("muj_config", " ", "application/pdf")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(supabaseStorageService, never()).createSignedUploadUrl(anyString());
    }

    @Test
    void registerFile_Success_SkipsUploadAndReindexes() {
        Config config = new Config();
        config.setId(1L);
        config.setCustomText("text");

        when(configRepository.findByIdWithFiles(1L)).thenReturn(Optional.of(config));
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));

        File result = configService.registerFile(1L, "configs/1/uuid_smlouva.pdf", "smlouva.pdf", "application/pdf");

        assertNotNull(result);
        assertEquals("configs/1/uuid_smlouva.pdf", result.getStoragePath());
        assertEquals("smlouva.pdf", result.getFileName());
        assertEquals("application/pdf", result.getFileType());

        // bajty souboru už jsou ve Storage nahrané klientem přímo - appka je znovu neposílá
        verify(supabaseStorageService, never()).uploadFile(anyString(), any(), anyString());
        verify(fileRepository, times(1)).save(any(File.class));
        verify(vectorServiceClient, times(1)).indexConfig(eq(1L), eq("text"), any(), anyList());
    }

    @Test
    void registerFile_ThrowsException_WhenConfigNotFound() {
        when(configRepository.findByIdWithFiles(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.registerFile(99L, "configs/99/a.pdf", "a.pdf", "application/pdf")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(fileRepository, never()).save(any());
    }
}
