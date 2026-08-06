package org.config.unit.facade;

import org.config.data.model.Config;
import org.config.dto.ConfigDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigFacadeFindAllTest extends BaseConfigFacadeTest {

    @Test
    void findAllNames_Success() {
        List<String> names = List.of("config1", "config2");
        when(configService.findAllNames()).thenReturn(names);

        List<String> result = configFacade.findAllNames();

        assertEquals(2, result.size());
        assertTrue(result.contains("config1"));
        verify(configService).findAllNames();
    }

    @Test
    void findAll_Success() {
        Config entity1 = new Config();
        Config entity2 = new Config();

        ConfigDto dto1 = new ConfigDto("config1", "{\"v\":1}");
        ConfigDto dto2 = new ConfigDto("config2", "{\"v\":2}");

        when(configService.findAll()).thenReturn(List.of(entity1, entity2));
        when(configMapper.toDto(entity1)).thenReturn(dto1);
        when(configMapper.toDto(entity2)).thenReturn(dto2);

        List<ConfigDto> result = configFacade.findAll();

        assertEquals(2, result.size());
        assertEquals("config1", result.get(0).name());
        assertEquals("config2", result.get(1).name());

        verify(configService).findAll();
        verify(configMapper, times(2)).toDto(any());
    }
}
