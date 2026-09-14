package org.config.unit.service;

import org.config.data.model.Config;
import org.config.data.model.File;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ConfigServiceFileDownloadUrlTest extends BaseConfigServiceTest {

    @Test
    void getFileDownloadUrl_Success_ReturnsSignedUrlForStoragePath() {
        Config config = new Config();
        config.setId(1L);
        File file = new File(config, "configs/1/uuid_smlouva.pdf", "smlouva.pdf", "application/pdf");

        when(fileRepository.findByConfigIdAndFileName(1L, "smlouva.pdf")).thenReturn(Optional.of(file));
        when(supabaseStorageService.createSignedDownloadUrl("configs/1/uuid_smlouva.pdf"))
                .thenReturn("https://supabase.example/storage/v1/object/sign/config-files/configs/1/uuid_smlouva.pdf?token=abc");

        String result = configService.getFileDownloadUrl(1L, "smlouva.pdf");

        assertEquals("https://supabase.example/storage/v1/object/sign/config-files/configs/1/uuid_smlouva.pdf?token=abc", result);
        verify(supabaseStorageService, times(1)).createSignedDownloadUrl("configs/1/uuid_smlouva.pdf");
    }

    @Test
    void getFileDownloadUrl_ThrowsException_WhenFileNotFoundForConfig() {
        when(fileRepository.findByConfigIdAndFileName(1L, "neexistuje.pdf")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.getFileDownloadUrl(1L, "neexistuje.pdf")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(supabaseStorageService, never()).createSignedDownloadUrl(anyString());
    }

    @Test
    void getFileDownloadUrl_LooksUpFileScopedToConfig_NotByNameAlone() {
        // stejné jméno souboru u jiného configu nesmí ovlivnit výsledek - hledá se přes (configId, fileName)
        when(fileRepository.findByConfigIdAndFileName(2L, "smlouva.pdf")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> configService.getFileDownloadUrl(2L, "smlouva.pdf"));

        verify(fileRepository, times(1)).findByConfigIdAndFileName(2L, "smlouva.pdf");
        verify(fileRepository, never()).findByStoragePath(anyString());
    }
}
