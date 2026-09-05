package org.config.unit.facade;

import org.config.dto.ConfigDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class ConfigFacadeUpdateTest extends BaseConfigFacadeTest {

    @Test
    void updateConfig_Success() {
        String name = "my_config";
        ConfigDto dto = new ConfigDto(
                name,
                "Popis konfigurace",
                5000,
                "Mozilla/5.0",
                "https://example.com",
                List.of()
        );

        assertDoesNotThrow(() -> configFacade.updateConfig(name, dto));

        verify(configService).updateConfig(eq(name), any());
    }
}
