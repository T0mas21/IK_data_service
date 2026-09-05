package org.config.unit.rest;

import org.config.dto.ConfigDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigApiCreateTest extends BaseConfigApiTest {

    @Test
    void createConfig_ShouldReturn201CreatedAndBody() {
        ConfigDto inputDto = new ConfigDto(
                "app_setting",
                "Aplikacni nastaveni",
                5000,
                "Mozilla/5.0",
                "https://example.com",
                List.of()
        );

        ConfigDto createdDto = new ConfigDto(
                "app_setting",
                "Aplikacni nastaveni",
                5000,
                "Mozilla/5.0",
                "https://example.com",
                List.of()
        );

        when(configFacade.createConfig(inputDto)).thenReturn(createdDto);

        ResponseEntity<ConfigDto> response = configApi.createConfig(inputDto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("app_setting", response.getBody().name());
        assertEquals("Aplikacni nastaveni", response.getBody().description());
        assertEquals(5000, response.getBody().timeout());
        assertEquals("Mozilla/5.0", response.getBody().userAgent());
        assertEquals("https://example.com", response.getBody().url());

        verify(configFacade).createConfig(inputDto);
    }
}