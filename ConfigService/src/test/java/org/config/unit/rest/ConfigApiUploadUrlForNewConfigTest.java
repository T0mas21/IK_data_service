package org.config.unit.rest;

import org.config.dto.UploadUrlByNameRequestDto;
import org.config.dto.UploadUrlDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigApiUploadUrlForNewConfigTest extends BaseConfigApiTest {

    @Test
    void createUploadUrlForNewConfig_ShouldReturn200OKAndUploadUrl() {
        UploadUrlByNameRequestDto request = new UploadUrlByNameRequestDto("muj_config", "smlouva.pdf", "application/pdf");
        UploadUrlDto expected = new UploadUrlDto(
                "configs/muj_config/uuid_smlouva.pdf",
                "https://supabase.example/storage/v1/object/upload/sign/config-files/configs/muj_config/uuid_smlouva.pdf?token=abc",
                "smlouva.pdf",
                "application/pdf"
        );

        when(configFacade.createUploadUrlForNewConfig(request)).thenReturn(expected);

        ResponseEntity<UploadUrlDto> response = configApi.createUploadUrlForNewConfig(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(configFacade, times(1)).createUploadUrlForNewConfig(request);
    }
}
