package org.config.unit.rest;

import org.config.dto.ConfigDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigApiCreateTest extends BaseConfigApiTest {

    @Test
    void createConfig_ShouldReturn201CreatedAndBody() {
        ConfigDto inputDto = new ConfigDto("app_setting", "{\"theme\":\"dark\"}");
        ConfigDto createdDto = new ConfigDto("app_setting", "{\"theme\":\"dark\"}");

        when(configFacade.createConfig(inputDto)).thenReturn(createdDto);

        ResponseEntity<ConfigDto> response = configApi.createConfig(inputDto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("app_setting", response.getBody().name());
        verify(configFacade).createConfig(inputDto);
    }
}
