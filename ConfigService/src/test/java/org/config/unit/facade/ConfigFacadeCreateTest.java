package org.config.unit.facade;

import org.config.data.model.Config;
import org.config.dto.ConfigDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigFacadeCreateTest extends BaseConfigFacadeTest {

    @Test
    void createConfig_Success() {
        ConfigDto inputDto = new ConfigDto(
                "test_config",
                "Popis konfigurace",
                5000,
                "Mozilla/5.0",
                "https://example.com",
                List.of()
        );

        Config entity = new Config();
        entity.setName("test_config");
        entity.setDescription("Popis konfigurace");
        entity.setTimeout(5000);
        entity.setUserAgent("Mozilla/5.0");
        entity.setUrl("https://example.com");

        Config savedEntity = new Config();
        savedEntity.setId(1L);
        savedEntity.setName("test_config");
        savedEntity.setDescription("Popis konfigurace");
        savedEntity.setTimeout(5000);
        savedEntity.setUserAgent("Mozilla/5.0");
        savedEntity.setUrl("https://example.com");

        ConfigDto expectedDto = new ConfigDto(
                "test_config",
                "Popis konfigurace",
                5000,
                "Mozilla/5.0",
                "https://example.com",
                List.of()
        );

        when(configMapper.toEntity(inputDto)).thenReturn(entity);
        when(configService.createConfig(entity)).thenReturn(savedEntity);
        when(configMapper.toDto(savedEntity)).thenReturn(expectedDto);

        ConfigDto result = configFacade.createConfig(inputDto);

        assertNotNull(result);
        assertEquals("test_config", result.name());
        assertEquals("Popis konfigurace", result.description());
        assertEquals(5000, result.timeout());
        assertEquals("Mozilla/5.0", result.userAgent());
        assertEquals("https://example.com", result.url());

        verify(configMapper).toEntity(inputDto);
        verify(configService).createConfig(entity);
        verify(configMapper).toDto(savedEntity);
    }
}
