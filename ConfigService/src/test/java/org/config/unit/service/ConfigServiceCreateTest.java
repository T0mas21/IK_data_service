package org.config.unit.service;

import org.config.data.model.Config;
import org.config.data.model.File;
import org.config.dto.FileDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConfigServiceCreateTest extends BaseConfigServiceTest {

    @Test
    void createConfig_Success() {
        Config config = new Config();
        config.setName("valid_config");
        config.setDescription("Testovací popis");
        config.setTimeout(5000);
        config.setUserAgent("Mozilla/5.0");
        config.setUrl("https://example.com");

        when(configRepository.findByName("valid_config")).thenReturn(Optional.empty());
        when(configRepository.save(any(Config.class))).thenReturn(config);

        Config result = configService.createConfig(config, List.of());

        assertNotNull(result);
        assertEquals("valid_config", result.getName());
        assertEquals("Testovací popis", result.getDescription());
        assertEquals(5000, result.getTimeout());
        assertEquals("Mozilla/5.0", result.getUserAgent());
        assertEquals("https://example.com", result.getUrl());

        verify(configRepository, times(1)).save(config);
    }

    @Test
    void createConfig_ThrowsException_WhenNameIsNull() {
        Config config = new Config();
        config.setName(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.createConfig(config, List.of())
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Jméno konfiguračního souboru nesmí být prázdné.", exception.getReason());
        verify(configRepository, never()).save(any());
    }

    @Test
    void createConfig_ThrowsException_WhenNameIsBlank() {
        Config config = new Config();
        config.setName("   ");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.createConfig(config, List.of())
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Jméno konfiguračního souboru nesmí být prázdné.", exception.getReason());
        verify(configRepository, never()).save(any());
    }

    @Test
    void createConfig_ThrowsException_WhenAlreadyExists() {
        Config config = new Config();
        config.setName("existing_config");

        when(configRepository.findByName("existing_config")).thenReturn(Optional.of(config));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.createConfig(config, List.of())
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Konfigurační soubor 'existing_config' již existuje.", exception.getReason());
        verify(configRepository, never()).save(any());
    }

    @Test
    void createConfig_Success_WithFileAlreadyUploadedViaSignedUrl() {
        Config config = new Config();
        config.setName("config_with_files");

        FileDto preUploadedFile = new FileDto(null, "smlouva.pdf", "configs/config_with_files/uuid_smlouva.pdf", "application/pdf", null);

        when(configRepository.findByName("config_with_files")).thenReturn(Optional.empty());
        when(configRepository.save(any(Config.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Config result = configService.createConfig(config, List.of(preUploadedFile));

        assertNotNull(result);
        assertEquals(1, result.getFiles().size());
        assertEquals("configs/config_with_files/uuid_smlouva.pdf", result.getFiles().get(0).getStoragePath());
        verify(configRepository, times(1)).save(any(Config.class));
        verify(supabaseStorageService, never()).uploadFile(anyString(), any(), anyString());
    }

    @Test
    void createConfig_UploadsFile_WhenContentProvided() {
        Config config = new Config();
        config.setName("config_with_base64_file");

        Config savedConfig = new Config();
        savedConfig.setId(1L);
        savedConfig.setName("config_with_base64_file");

        String base64Content = Base64.getEncoder().encodeToString("obsah souboru".getBytes());
        FileDto newFile = new FileDto(null, "smlouva.pdf", null, "application/pdf", base64Content);

        when(configRepository.findByName("config_with_base64_file")).thenReturn(Optional.empty());
        when(configRepository.save(any(Config.class))).thenReturn(savedConfig);
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(supabaseStorageService.uploadFile(anyString(), any(byte[].class), eq("application/pdf")))
                .thenReturn("configs/1/uuid_smlouva.pdf");

        Config result = configService.createConfig(config, List.of(newFile));

        assertEquals(1, result.getFiles().size());
        assertEquals("smlouva.pdf", result.getFiles().get(0).getFileName());
        verify(supabaseStorageService).uploadFile(anyString(), any(byte[].class), eq("application/pdf"));
        verify(fileRepository).save(any(File.class));
    }

    @Test
    void createConfig_ThrowsException_WhenFileHasNeitherContentNorStoragePath() {
        Config config = new Config();
        config.setName("config_with_bad_file");

        FileDto badFile = new FileDto(null, "smlouva.pdf", null, "application/pdf", null);

        when(configRepository.findByName("config_with_bad_file")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.createConfig(config, List.of(badFile))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(configRepository, never()).save(any());
    }

    @Test
    void createConfig_ThrowsException_WhenFileHasBlankFileName() {
        Config config = new Config();
        config.setName("config_with_bad_file");

        FileDto badFile = new FileDto(null, "  ", "configs/config_with_bad_file/uuid.pdf", "application/pdf", null);

        when(configRepository.findByName("config_with_bad_file")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.createConfig(config, List.of(badFile))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(configRepository, never()).save(any());
    }

    @Test
    void createConfig_ThrowsException_WhenContentIsNotValidBase64() {
        Config config = new Config();
        config.setName("config_with_bad_file");

        FileDto badFile = new FileDto(null, "smlouva.pdf", null, "application/pdf", "not-valid-base64!@#");

        when(configRepository.findByName("config_with_bad_file")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.createConfig(config, List.of(badFile))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(configRepository, never()).save(any());
    }
}
