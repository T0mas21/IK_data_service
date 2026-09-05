package org.config.unit.facade;

import org.config.data.model.Config;
import org.config.dto.ConfigDto;
import org.config.dto.ConfigNamesDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigFacadeFindAllTest extends BaseConfigFacadeTest {

    @Test
    void findAllNames_Success() {
        List<String> names = List.of("config1", "config2");
        when(configService.findAllNames()).thenReturn(names);

        ConfigNamesDto result = configFacade.findAllNames();

        assertNotNull(result);
        assertNotNull(result.getNames());
        assertEquals(2, result.getNames().size());
        assertEquals("config1", result.getNames().get(0).getName());
        assertEquals("config2", result.getNames().get(1).getName());

        verify(configService, times(1)).findAllNames();
    }

    @Test
    void findAll_Success() {
        Config entity1 = new Config();
        entity1.setName("config1");
        entity1.setDescription("Popis 1");
        entity1.setTimeout(1000);
        entity1.setUserAgent("Agent 1");
        entity1.setUrl("https://example1.com");

        Config entity2 = new Config();
        entity2.setName("config2");
        entity2.setDescription("Popis 2");
        entity2.setTimeout(2000);
        entity2.setUserAgent("Agent 2");
        entity2.setUrl("https://example2.com");

        ConfigDto dto1 = new ConfigDto("config1", "Popis 1", 1000, "Agent 1", "https://example1.com", List.of());
        ConfigDto dto2 = new ConfigDto("config2", "Popis 2", 2000, "Agent 2", "https://example2.com", List.of());

        when(configService.findAll()).thenReturn(List.of(entity1, entity2));
        when(configMapper.toDto(entity1)).thenReturn(dto1);
        when(configMapper.toDto(entity2)).thenReturn(dto2);

        List<ConfigDto> result = configFacade.findAll();

        assertEquals(2, result.size());
        assertEquals("config1", result.get(0).name());
        assertEquals("Popis 1", result.get(0).description());
        assertEquals("config2", result.get(1).name());
        assertEquals("Popis 2", result.get(1).description());

        verify(configService).findAll();
        verify(configMapper).toDto(entity1);
        verify(configMapper).toDto(entity2);
    }
}
