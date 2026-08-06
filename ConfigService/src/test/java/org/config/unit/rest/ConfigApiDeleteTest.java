package org.config.unit.rest;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

class ConfigApiDeleteTest extends BaseConfigApiTest {

    @Test
    void deleteByName_ShouldReturn204NoContent() {
        String name = "config_to_delete";

        ResponseEntity<Void> response = configApi.deleteByName(name);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(configFacade).deleteByName(name);
    }
}