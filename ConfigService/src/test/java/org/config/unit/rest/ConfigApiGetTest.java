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
        ConfigDto expectedDto = new ConfigDto(
                name,
                "Aplikacni nastaveni",
                5000,
                "Mozilla/5.0",
                "https://example.com"
        );

        when(configFacade.findByName(name)).thenReturn(expectedDto);

        ResponseEntity<ConfigDto> response = configApi.getByName(name);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(name, response.getBody().name());
        assertEquals("Aplikacni nastaveni", response.getBody().description());
        assertEquals(5000, response.getBody().timeout());
        assertEquals("Mozilla/5.0", response.getBody().userAgent());
        assertEquals("https://example.com", response.getBody().url());

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
                new ConfigDto("config1", "Popis 1", 1000, "Agent 1", "https://example1.com"),
                new ConfigDto("config2", "Popis 2", 2000, "Agent 2", "https://example2.com")
        );
        when(configFacade.findAll()).thenReturn(configs);

        ResponseEntity<List<ConfigDto>> response = configApi.getAll();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("config1", response.getBody().get(0).name());
        assertEquals("config2", response.getBody().get(1).name());

        verify(configFacade).findAll();
    }
}