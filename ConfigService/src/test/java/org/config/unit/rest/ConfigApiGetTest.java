package org.config.unit.rest;

import org.config.dto.ConfigDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigApiGetTest extends BaseConfigApiTest {

    @Test
    void getByName_ShouldReturn200OKAndConfig() {
        String name = "app_setting";
        ConfigDto expectedDto = new ConfigDto(name, "{\"theme\":\"dark\"}");

        when(configFacade.findByName(name)).thenReturn(expectedDto);

        ResponseEntity<ConfigDto> response = configApi.getByName(name);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(name, response.getBody().name());
        verify(configFacade).findByName(name);
    }

    @Test
    void getAllNames_ShouldReturn200OKAndListOfNames() {
        List<String> names = List.of("config1", "config2");
        when(configFacade.findAllNames()).thenReturn(names);

        ResponseEntity<List<String>> response = configApi.getAllNames();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().contains("config1"));
        verify(configFacade).findAllNames();
    }

    @Test
    void getAll_ShouldReturn200OKAndListOfConfigs() {
        List<ConfigDto> configs = List.of(
                new ConfigDto("config1", "{\"v\":1}"),
                new ConfigDto("config2", "{\"v\":2}")
        );
        when(configFacade.findAll()).thenReturn(configs);

        ResponseEntity<List<ConfigDto>> response = configApi.getAll();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(configFacade).findAll();
    }
}