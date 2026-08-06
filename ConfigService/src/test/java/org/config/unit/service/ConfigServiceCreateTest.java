package org.config.unit.service;

import org.config.data.model.Config;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConfigServiceCreateTest extends BaseConfigServiceTest {

    @Test
    void createConfig_Success() {
        Config config = new Config();
        config.setName("valid_config");

        when(configRepository.findByName("valid_config")).thenReturn(Optional.empty());
        when(configRepository.save(any(Config.class))).thenReturn(config);

        Config result = configService.createConfig(config);

        assertNotNull(result);
        assertEquals("valid_config", result.getName());
        verify(configRepository, times(1)).save(config);
    }

    @Test
    void createConfig_ThrowsException_WhenNameIsNull() {
        Config config = new Config();
        config.setName(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> configService.createConfig(config)
        );

        assertEquals("Jméno konfiguračního souboru nesmí být prázdné.", exception.getMessage());
        verify(configRepository, never()).save(any());
    }

    @Test
    void createConfig_ThrowsException_WhenNameIsBlank() {
        Config config = new Config();
        config.setName("   ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> configService.createConfig(config)
        );

        assertEquals("Jméno konfiguračního souboru nesmí být prázdné.", exception.getMessage());
        verify(configRepository, never()).save(any());
    }

    @Test
    void createConfig_ThrowsException_WhenAlreadyExists() {
        Config config = new Config();
        config.setName("existing_config");

        when(configRepository.findByName("existing_config")).thenReturn(Optional.of(config));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> configService.createConfig(config)
        );

        assertEquals("Konfigurační soubor již existuje.", exception.getMessage());
        verify(configRepository, never()).save(any());
    }
}
