package org.config.unit.facade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigFacadeFileDownloadUrlTest extends BaseConfigFacadeTest {

    @Test
    void getFileDownloadUrl_DelegatesToConfigService() {
        when(configService.getFileDownloadUrl(1L, "smlouva.pdf"))
                .thenReturn("https://supabase.example/storage/v1/object/sign/config-files/configs/1/uuid_smlouva.pdf?token=abc");

        String result = configFacade.getFileDownloadUrl(1L, "smlouva.pdf");

        assertEquals("https://supabase.example/storage/v1/object/sign/config-files/configs/1/uuid_smlouva.pdf?token=abc", result);
        verify(configService, times(1)).getFileDownloadUrl(1L, "smlouva.pdf");
    }
}
