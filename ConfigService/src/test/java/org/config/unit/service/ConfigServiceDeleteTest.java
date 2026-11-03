package org.config.unit.service;

import org.config.data.model.Config;
import org.config.data.model.File;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigServiceDeleteTest extends BaseConfigServiceTest {

    @Test
    void deleteByName_Success() {
        String name = "config_to_delete";

        when(configRepository.findByNameWithFiles(name)).thenReturn(Optional.of(new Config()));

        assertDoesNotThrow(() -> configService.deleteByName(name));

        verify(configRepository).findByNameWithFiles(name);
        verify(configRepository, times(1)).deleteByName(name);
    }

    @Test
    void deleteByName_DeletesAllFilesFromStorageBeforeDbDelete() {
        String name = "config_with_files";
        Config config = new Config();
        config.addFile(new File(config, "configs/1/a_doc.pdf", "doc.pdf", "application/pdf"));
        config.addFile(new File(config, "configs/1/b_data.csv", "data.csv", "text/csv"));

        when(configRepository.findByNameWithFiles(name)).thenReturn(Optional.of(config));

        configService.deleteByName(name);

        InOrder inOrder = inOrder(supabaseStorageService, configRepository);
        inOrder.verify(supabaseStorageService).deleteFile("configs/1/a_doc.pdf");
        inOrder.verify(supabaseStorageService).deleteFile("configs/1/b_data.csv");
        inOrder.verify(configRepository).deleteByName(name);
    }

    @Test
    void deleteByName_WhenStorageDeleteFails_DoesNotDeleteConfig() {
        String name = "config_storage_down";
        Config config = new Config();
        config.addFile(new File(config, "configs/1/a_doc.pdf", "doc.pdf", "application/pdf"));

        when(configRepository.findByNameWithFiles(name)).thenReturn(Optional.of(config));
        doThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Smazání souboru ze Supabase Storage selhalo"))
                .when(supabaseStorageService).deleteFile("configs/1/a_doc.pdf");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.deleteByName(name)
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        verify(configRepository, never()).deleteByName(anyString());
        verify(vectorServiceClient, never()).deleteConfig(any());
    }

    @Test
    void deleteByName_ThrowsException_WhenNotFound() {
        String name = "unknown_config";

        when(configRepository.findByNameWithFiles(name)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.deleteByName(name)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Konfigurační soubor '" + name + "' neexistuje.", exception.getReason());

        verify(configRepository, never()).deleteByName(anyString());
        verify(supabaseStorageService, never()).deleteFile(anyString());
    }
}
