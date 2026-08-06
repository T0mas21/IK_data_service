package org.config.unit.facade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;

class ConfigFacadeUpdateTest extends BaseConfigFacadeTest {

    @Test
    void updateDataByName_Success() {
        String name = "my_config";
        String newData = "{\"key\":\"val\"}";

        assertDoesNotThrow(() -> configFacade.updateDataByName(name, newData));
        verify(configService).updateDataByName(name, newData);
    }
}
