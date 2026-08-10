package org.config.unit.service;

import org.config.data.model.Config;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ConfigServiceFindByNameTest extends BaseConfigServiceTest {

    @Test
    void findByName_Success() {
        Config config = new Config();
        config.setName("my_config");
        config.setDescription("Testovací popis");
        config.setTimeout(3000);
        config.setUserAgent("Mozilla/5.0");
        config.setUrl("https://example.com");

        when(configRepository.findByName("my_config")).thenReturn(Optional.of(config));

        Config result = configService.findByName("my_config");

        assertNotNull(result);
        assertEquals("my_config", result.getName());
        assertEquals("Testovací popis", result.getDescription());
        assertEquals(3000, result.getTimeout());
        assertEquals("Mozilla/5.0", result.getUserAgent());
        assertEquals("https://example.com", result.getUrl());
    }

    @Test
    void findByName_ThrowsException_WhenNotFound() {
        when(configRepository.findByName("unknown")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.findByName("unknown")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Konfigurace 'unknown' nebyla nalezena.", exception.getReason());
    }
}