package org.config.unit.facade;

import org.config.data.model.Config;
import org.config.dto.ConfigDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigFacadeFindByNameTest extends BaseConfigFacadeTest {

    @Test
    void findByName_Success() {
        String name = "my_config";
        Config entity = new Config();
        entity.setName(name);
        entity.setDescription("Popis konfigurace");
        entity.setTimeout(3000);
        entity.setUserAgent("Mozilla/5.0");
        entity.setUrl("https://example.com");

        ConfigDto expectedDto = new ConfigDto(
                name,
                "Popis konfigurace",
                3000,
                "Mozilla/5.0",
                "https://example.com"
        );

        when(configService.findByName(name)).thenReturn(entity);
        when(configMapper.toDto(entity)).thenReturn(expectedDto);

        ConfigDto result = configFacade.findByName(name);

        assertNotNull(result);
        assertEquals(name, result.name());
        assertEquals("Popis konfigurace", result.description());
        assertEquals(3000, result.timeout());
        assertEquals("Mozilla/5.0", result.userAgent());
        assertEquals("https://example.com", result.url());

        verify(configService).findByName(name);
        verify(configMapper).toDto(entity);
    }

    @Test
    void findByName_ThrowsException_WhenServiceFails() {
        String name = "unknown";
        when(configService.findByName(name)).thenThrow(new RuntimeException("Not found"));

        assertThrows(RuntimeException.class, () -> configFacade.findByName(name));

        verify(configService).findByName(name);
        verifyNoInteractions(configMapper);
    }
}