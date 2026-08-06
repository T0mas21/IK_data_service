package org.config.unit.service;

import org.config.data.model.Config;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ConfigServiceFindAllTest extends BaseConfigServiceTest {

    @Test
    void findAllNames_Success() {
        List<String> expectedNames = List.of("config1", "config2");
        when(configRepository.findAllNames()).thenReturn(expectedNames);

        List<String> result = configService.findAllNames();

        assertEquals(2, result.size());
        assertTrue(result.contains("config1"));
    }

    @Test
    void findAll_Success() {
        Config c1 = new Config();
        Config c2 = new Config();
        when(configRepository.findAll()).thenReturn(List.of(c1, c2));

        List<Config> result = configService.findAll();

        assertEquals(2, result.size());
    }
}