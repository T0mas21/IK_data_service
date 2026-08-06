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

        ConfigDto expectedDto = new ConfigDto(name, "{\"key\":\"val\"}");

        when(configService.findByName(name)).thenReturn(entity);
        when(configMapper.toDto(entity)).thenReturn(expectedDto);

        ConfigDto result = configFacade.findByName(name);

        assertNotNull(result);
        assertEquals(name, result.name());
        assertEquals("{\"key\":\"val\"}", result.data());

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