package org.config.unit.service;

import org.config.data.model.Config;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

        when(configRepository.findByName(name)).thenReturn(Optional.of(existingConfig));

        Config result = configService.updateConfig(name, updatedConfig);

        assertNotNull(result);
        assertEquals("Nový popis", result.getDescription());
        assertEquals(3000, result.getTimeout());
        assertEquals("Mozilla/5.0", result.getUserAgent());
        assertEquals("https://example.com", result.getUrl());

        verify(configRepository).findByName(name);
    }

    @Test
    void updateConfig_ThrowsException_WhenNotFound() {
        String name = "non_existing";
        Config updatedConfig = new Config();

        when(configRepository.findByName(name)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.updateConfig(name, updatedConfig)
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

        when(configRepository.findByName(name)).thenReturn(Optional.of(existingConfig));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> configService.updateConfig(name, updatedConfig)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Timeout nesmí být záporná hodnota.", exception.getReason());
    }
}