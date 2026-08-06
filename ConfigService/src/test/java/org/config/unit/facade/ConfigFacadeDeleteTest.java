package org.config.unit.facade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;

class ConfigFacadeDeleteTest extends BaseConfigFacadeTest {

    @Test
    void deleteByName_Success() {
        String name = "config_to_delete";

        assertDoesNotThrow(() -> configFacade.deleteByName(name));
        verify(configService).deleteByName(name);
    }
}
