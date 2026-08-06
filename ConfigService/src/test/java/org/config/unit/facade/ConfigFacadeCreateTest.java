package org.config.unit.facade;

import org.config.data.model.Config;
import org.config.dto.ConfigDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigFacadeCreateTest extends BaseConfigFacadeTest {

    @Test
    void createConfig_Success() {
        ConfigDto inputDto = new ConfigDto("test_config", "{\"theme\":\"dark\"}");
        Config entity = new Config();
        entity.setName("test_config");

        Config savedEntity = new Config();
        savedEntity.setId(1L);
        savedEntity.setName("test_config");

        ConfigDto expectedDto = new ConfigDto("test_config", "{\"theme\":\"dark\"}");

        when(configMapper.toEntity(inputDto)).thenReturn(entity);
        when(configService.createConfig(entity)).thenReturn(savedEntity);
        when(configMapper.toDto(savedEntity)).thenReturn(expectedDto);

        ConfigDto result = configFacade.createConfig(inputDto);

        assertNotNull(result);
        assertEquals("test_config", result.name());
    }
}
