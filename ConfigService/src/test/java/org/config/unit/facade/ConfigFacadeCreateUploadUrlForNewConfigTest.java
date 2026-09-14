package org.config.unit.facade;

import org.config.dto.UploadUrlByNameRequestDto;
import org.config.dto.UploadUrlDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigFacadeCreateUploadUrlForNewConfigTest extends BaseConfigFacadeTest {

    @Test
    void createUploadUrlForNewConfig_DelegatesToConfigService() {
        UploadUrlByNameRequestDto request = new UploadUrlByNameRequestDto("muj_config", "smlouva.pdf", "application/pdf");
        UploadUrlDto expected = new UploadUrlDto(
                "configs/muj_config/uuid_smlouva.pdf",
                "https://supabase.example/storage/v1/object/upload/sign/config-files/configs/muj_config/uuid_smlouva.pdf?token=abc",
                "smlouva.pdf",
                "application/pdf"
        );

        when(configService.createUploadUrlForNewConfig("muj_config", "smlouva.pdf", "application/pdf"))
                .thenReturn(expected);

        UploadUrlDto result = configFacade.createUploadUrlForNewConfig(request);

        assertEquals(expected, result);
        verify(configService, times(1)).createUploadUrlForNewConfig("muj_config", "smlouva.pdf", "application/pdf");
    }
}
