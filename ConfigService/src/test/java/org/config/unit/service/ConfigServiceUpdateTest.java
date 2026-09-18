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

class ConfigServiceUpdateTest extends BaseConfigServiceTest {

    @Test
    void updateConfig_Success() {
        String name = "my_config";
        Config existingConfig = new Config();
        existingConfig.setName(name);

        Config updatedConfig = new Config();
        updatedConfig.setDescription("Nový popis");
        updatedConfig.setTimeout(3000);
        updatedConfig.setUserAgent("Mozilla/5.0");
        updatedConfig.setUrl("https://example.com");

        when(configRepository.findByNameWithFiles(name)).thenReturn(Optional.of(existingConfig));

        Config result = configService.updateConfig(name, updatedConfig, null);

        assertNotNull(result);
        assertEquals("Nový popis", result.getDescription());
        assertEquals(3000, result.getTimeout());
        assertEquals("Mozilla/5.0", result.getUserAgent());
        assertEquals("https://example.com", result.getUrl());

        verify(configRepository).findByNameWithFiles(name);
    }

    @Test
    void updateConfig_ThrowsException_WhenNotFound() {
        String name = "non_existing";
        Config updatedConfig = new Config();

        when(configRepository.findByNameWithFiles(name)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.updateConfig(name, updatedConfig, null)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Konfigurační soubor '" + name + "' neexistuje.", exception.getReason());
    }

    @Test
    void updateConfig_ThrowsException_WhenTimeoutIsNegative() {
        String name = "my_config";
        Config existingConfig = new Config();
        existingConfig.setName(name);

        Config updatedConfig = new Config();
        updatedConfig.setTimeout(-100);

        when(configRepository.findByNameWithFiles(name)).thenReturn(Optional.of(existingConfig));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.updateConfig(name, updatedConfig, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Timeout nesmí být záporná hodnota.", exception.getReason());
    }

    @Test
    void updateConfig_UploadsNewFile_WhenContentProvided() {
        String name = "my_config";
        Config existingConfig = new Config();
        existingConfig.setId(1L);
        existingConfig.setName(name);

        String base64Content = Base64.getEncoder().encodeToString("obsah souboru".getBytes());
        FileDto newFile = new FileDto(null, "smlouva.pdf", null, "application/pdf", base64Content);

        when(configRepository.findByNameWithFiles(name)).thenReturn(Optional.of(existingConfig));
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(supabaseStorageService.uploadFile(anyString(), any(byte[].class), eq("application/pdf")))
                .thenReturn("configs/1/uuid_smlouva.pdf");

        Config result = configService.updateConfig(name, new Config(), List.of(newFile));

        assertEquals(1, result.getFiles().size());
        assertEquals("smlouva.pdf", result.getFiles().get(0).getFileName());
        verify(supabaseStorageService).uploadFile(anyString(), any(byte[].class), eq("application/pdf"));
        verify(fileRepository).save(any(File.class));
    }

    @Test
    void updateConfig_ReplacesExistingFile_WhenSameNameWithContent() {
        String name = "my_config";
        Config existingConfig = new Config();
        existingConfig.setId(1L);
        existingConfig.setName(name);
        File oldFile = new File(existingConfig, "configs/1/old_smlouva.pdf", "smlouva.pdf", "application/pdf");
        existingConfig.addFile(oldFile);

        String base64Content = Base64.getEncoder().encodeToString("novy obsah".getBytes());
        FileDto replacementFile = new FileDto(null, "smlouva.pdf", null, "application/pdf", base64Content);

        when(configRepository.findByNameWithFiles(name)).thenReturn(Optional.of(existingConfig));
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(supabaseStorageService.uploadFile(anyString(), any(byte[].class), eq("application/pdf")))
                .thenReturn("configs/1/uuid_smlouva.pdf");

        Config result = configService.updateConfig(name, new Config(), List.of(replacementFile));

        assertEquals(1, result.getFiles().size());
        verify(supabaseStorageService).deleteFile("configs/1/old_smlouva.pdf");
        verify(fileRepository).delete(oldFile);
        verify(supabaseStorageService).uploadFile(anyString(), any(byte[].class), eq("application/pdf"));
    }

    @Test
    void updateConfig_KeepsExistingFile_WhenReferencedWithoutContent() {
        String name = "my_config";
        Config existingConfig = new Config();
        existingConfig.setId(1L);
        existingConfig.setName(name);
        File oldFile = new File(existingConfig, "configs/1/old_smlouva.pdf", "smlouva.pdf", "application/pdf");
        existingConfig.addFile(oldFile);

        FileDto keptFile = new FileDto(null, "smlouva.pdf", null, null, null);

        when(configRepository.findByNameWithFiles(name)).thenReturn(Optional.of(existingConfig));

        Config result = configService.updateConfig(name, new Config(), List.of(keptFile));

        assertEquals(1, result.getFiles().size());
        assertEquals("configs/1/old_smlouva.pdf", result.getFiles().get(0).getStoragePath());
        verify(supabaseStorageService, never()).deleteFile(anyString());
        verify(supabaseStorageService, never()).uploadFile(anyString(), any(), anyString());
        verify(fileRepository, never()).delete(any());
    }

    @Test
    void updateConfig_DeletesFile_WhenOmittedFromRequest() {
        String name = "my_config";
        Config existingConfig = new Config();
        existingConfig.setId(1L);
        existingConfig.setName(name);
        File oldFile = new File(existingConfig, "configs/1/old_smlouva.pdf", "smlouva.pdf", "application/pdf");
        existingConfig.addFile(oldFile);

        when(configRepository.findByNameWithFiles(name)).thenReturn(Optional.of(existingConfig));

        Config result = configService.updateConfig(name, new Config(), List.of());

        assertTrue(result.getFiles().isEmpty());
        verify(supabaseStorageService).deleteFile("configs/1/old_smlouva.pdf");
        verify(fileRepository).delete(oldFile);
    }

    @Test
    void updateConfig_ThrowsException_WhenReferencedFileNotFoundAndNoContent() {
        String name = "my_config";
        Config existingConfig = new Config();
        existingConfig.setId(1L);
        existingConfig.setName(name);

        FileDto unknownFile = new FileDto(null, "neexistujici.pdf", null, null, null);

        when(configRepository.findByNameWithFiles(name)).thenReturn(Optional.of(existingConfig));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.updateConfig(name, new Config(), List.of(unknownFile))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(supabaseStorageService, never()).uploadFile(anyString(), any(), anyString());
    }

    @Test
    void updateConfig_ThrowsException_WhenContentIsNotValidBase64() {
        String name = "my_config";
        Config existingConfig = new Config();
        existingConfig.setId(1L);
        existingConfig.setName(name);

        FileDto invalidFile = new FileDto(null, "smlouva.pdf", null, "application/pdf", "not-valid-base64!@#");

        when(configRepository.findByNameWithFiles(name)).thenReturn(Optional.of(existingConfig));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.updateConfig(name, new Config(), List.of(invalidFile))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(fileRepository, never()).save(any());
    }
}