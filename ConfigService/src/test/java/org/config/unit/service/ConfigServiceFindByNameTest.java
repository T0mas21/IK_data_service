package org.config.unit.service;

import org.config.data.model.Config;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ConfigServiceFindByNameTest extends BaseConfigServiceTest {

    @Test
    void findByName_Success() {
        Config config = new Config();
        config.setName("my_config");

        when(configRepository.findByName("my_config")).thenReturn(Optional.of(config));

        Config result = configService.findByName("my_config");

        assertNotNull(result);
        assertEquals("my_config", result.getName());
    }

    @Test
    void findByName_ThrowsException_WhenNotFound() {
        when(configRepository.findByName("unknown")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> configService.findByName("unknown")
        );

        assertEquals("Konfigurace 'unknown' nebyla nalezena.", exception.getMessage());
    }
}
