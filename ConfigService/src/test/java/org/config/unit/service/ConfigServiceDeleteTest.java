package org.config.unit.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class ConfigServiceDeleteTest extends BaseConfigServiceTest {

    @Test
    void deleteByName_Success() {
        String name = "config_to_delete";

        assertDoesNotThrow(() -> configService.deleteByName(name));
        verify(configRepository, times(1)).deleteByName(name);
    }

    @Test
    void deleteByName_NonExisting_DoesNotFail() {
        String name = "unknown_config";

        // Současná implemetace Service neověřuje existenci před smazáním
        assertDoesNotThrow(() -> configService.deleteByName(name));
        verify(configRepository, times(1)).deleteByName(name);
    }
}