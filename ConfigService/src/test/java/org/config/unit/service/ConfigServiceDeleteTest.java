package org.config.unit.service;

import org.config.data.model.Config;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigServiceDeleteTest extends BaseConfigServiceTest {

    @Test
    void deleteByName_Success() {
        String name = "config_to_delete";

        when(configRepository.findByName(name)).thenReturn(Optional.of(new Config()));

        assertDoesNotThrow(() -> configService.deleteByName(name));

        verify(configRepository).findByName(name);
        verify(configRepository, times(1)).deleteByName(name);
    }

    @Test
    void deleteByName_ThrowsException_WhenNotFound() {
        String name = "unknown_config";

        when(configRepository.findByName(name)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.deleteByName(name)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Konfigurační soubor '" + name + "' neexistuje.", exception.getReason());

        verify(configRepository, never()).deleteByName(anyString());
    }
}