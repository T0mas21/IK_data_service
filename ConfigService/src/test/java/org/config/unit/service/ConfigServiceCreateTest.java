package org.config.unit.service;

import org.config.data.model.Config;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

        Config result = configService.createConfig(config);

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
                () -> configService.createConfig(config)
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
                () -> configService.createConfig(config)
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
                () -> configService.createConfig(config)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Konfigurační soubor 'existing_config' již existuje.", exception.getReason());
        verify(configRepository, never()).save(any());
    }
}