package org.config.unit.rest;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigApiFileDownloadTest extends BaseConfigApiTest {

    @Test
    void downloadFile_ShouldReturn302WithSignedUrlAsLocation() {
        String signedUrl = "https://supabase.example/storage/v1/object/sign/config-files/configs/1/uuid_smlouva.pdf?token=abc";
        when(configFacade.getFileDownloadUrl(1L, "smlouva.pdf")).thenReturn(signedUrl);

        ResponseEntity<Void> response = configApi.downloadFile(1L, "smlouva.pdf");

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals(URI.create(signedUrl), response.getHeaders().getLocation());
        verify(configFacade, times(1)).getFileDownloadUrl(1L, "smlouva.pdf");
    }
}
