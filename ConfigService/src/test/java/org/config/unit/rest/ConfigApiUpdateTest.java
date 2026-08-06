package org.config.unit.rest;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

class ConfigApiUpdateTest extends BaseConfigApiTest {

    @Test
    void updateDataByName_ShouldReturn204NoContent() {
        String name = "app_setting";
        String newData = "{\"theme\":\"light\"}";

        ResponseEntity<Void> response = configApi.updateDataByName(name, newData);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(configFacade).updateDataByName(name, newData);
    }
}
