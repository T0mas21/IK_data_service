package org.config.unit.rest;

import org.config.dto.ConfigDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

class ConfigApiUpdateTest extends BaseConfigApiTest {

    @Test
    void updateDataByName_ShouldReturn204NoContent() {
        String name = "app_setting";
        ConfigDto dto = new ConfigDto(
                name,
                "Popis",
                3000,
                "Mozilla/5.0",
                "https://example.com",
                List.of()
        );

        ResponseEntity<Void> response = configApi.updateConfig(name, dto);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(configFacade).updateConfig(name, dto);
    }
}
