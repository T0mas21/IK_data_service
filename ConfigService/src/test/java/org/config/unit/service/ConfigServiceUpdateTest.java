package org.config.unit.service;

import org.config.data.model.Config;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigServiceUpdateTest extends BaseConfigServiceTest {

    @Test
    void updateDataByName_Success() {
        String name = "my_config";
        String newData = "{\"key\":\"value\"}";

        when(configRepository.findByName(name)).thenReturn(Optional.of(new Config()));

        assertDoesNotThrow(() -> configService.updateDataByName(name, newData));
        verify(configRepository, times(1)).updateDataByName(name, newData);
    }

    @Test
    void updateDataByName_ThrowsException_WhenNotFound() {
        String name = "non_existing";
        when(configRepository.findByName(name)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> configService.updateDataByName(name, "some_data")
        );

        assertEquals("Konfigurační soubor neexistuje.", exception.getMessage());
        verify(configRepository, never()).updateDataByName(anyString(), anyString());
    }

    @Test
    void updateDataByName_ThrowsException_WhenNameIsInvalidOrBlank() {
        // Současná logika v Service vrství neověřuje 'name' na null/blank explicitně,
        // ale repozitář při prázdném jménu nic nenajde a vyhodí RuntimeException:
        when(configRepository.findByName("")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> configService.updateDataByName("", "data")
        );

        assertEquals("Konfigurační soubor neexistuje.", exception.getMessage());
        verify(configRepository, never()).updateDataByName(anyString(), anyString());
    }
}