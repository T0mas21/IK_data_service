package org.config.unit.facade;

import org.config.data.model.Config;
import org.config.dto.ConfigDto;
import org.config.dto.FileDto;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigFacadeFindByNameTest extends BaseConfigFacadeTest {

    @Test
    void findByName_Success() {
        String name = "my_config";
        Config entity = new Config();
        entity.setName(name);
        entity.setDescription("Popis konfigurace");
        entity.setTimeout(3000);
        entity.setUserAgent("Mozilla/5.0");
        entity.setUrl("https://example.com");

        ConfigDto expectedDto = new ConfigDto(
                name,
                "Popis konfigurace",
                3000,
                "Mozilla/5.0",
                "https://example.com",
                null,
                null,
                null,
                List.of()
        );

        when(configService.findByName(name)).thenReturn(entity);
        when(configMapper.toDto(entity)).thenReturn(expectedDto);

        ConfigDto result = configFacade.findByName(name);

        assertNotNull(result);
        assertEquals(name, result.name());
        assertEquals("Popis konfigurace", result.description());
        assertEquals(3000, result.timeout());
        assertEquals("Mozilla/5.0", result.userAgent());
        assertEquals("https://example.com", result.url());

        verify(configService).findByName(name);
        verify(configMapper).toDto(entity);
    }

    @Test
    void findByName_FillsFileContentFromStorage() {
        String name = "config_with_files";
        Config entity = new Config();
        entity.setName(name);

        FileDto fileWithoutContent = new FileDto(1L, "smlouva.pdf", "configs/1/uuid_smlouva.pdf", "application/pdf", null);
        ConfigDto mappedDto = new ConfigDto(name, null, null, null, null, null, null, null, List.of(fileWithoutContent));

        byte[] storedBytes = "obsah souboru".getBytes();

        when(configService.findByName(name)).thenReturn(entity);
        when(configMapper.toDto(entity)).thenReturn(mappedDto);
        when(configService.downloadFileContent("configs/1/uuid_smlouva.pdf")).thenReturn(storedBytes);

        ConfigDto result = configFacade.findByName(name);

        assertEquals(1, result.files().size());
        assertEquals(Base64.getEncoder().encodeToString(storedBytes), result.files().get(0).content());
        assertEquals("smlouva.pdf", result.files().get(0).fileName());
        assertEquals("configs/1/uuid_smlouva.pdf", result.files().get(0).storagePath());
        assertEquals("application/pdf", result.files().get(0).fileType());
        assertEquals(1L, result.files().get(0).id());
    }

    @Test
    void findByName_LeavesContentNull_WhenDownloadFails() {
        String name = "config_with_files";
        Config entity = new Config();
        entity.setName(name);

        FileDto fileWithoutContent = new FileDto(1L, "smlouva.pdf", "configs/1/uuid_smlouva.pdf", "application/pdf", null);
        ConfigDto mappedDto = new ConfigDto(name, null, null, null, null, null, null, null, List.of(fileWithoutContent));

        when(configService.findByName(name)).thenReturn(entity);
        when(configMapper.toDto(entity)).thenReturn(mappedDto);
        when(configService.downloadFileContent("configs/1/uuid_smlouva.pdf")).thenReturn(null);

        ConfigDto result = configFacade.findByName(name);

        assertEquals(1, result.files().size());
        assertNull(result.files().get(0).content());
        assertEquals("smlouva.pdf", result.files().get(0).fileName());
    }

    @Test
    void findByName_DoesNotTouchStorage_WhenConfigHasNoFiles() {
        String name = "config_without_files";
        Config entity = new Config();
        entity.setName(name);

        ConfigDto mappedDto = new ConfigDto(name, null, null, null, null, null, null, null, List.of());

        when(configService.findByName(name)).thenReturn(entity);
        when(configMapper.toDto(entity)).thenReturn(mappedDto);

        ConfigDto result = configFacade.findByName(name);

        assertTrue(result.files().isEmpty());
        verify(configService, never()).downloadFileContent(anyString());
    }

    @Test
    void findByName_ThrowsException_WhenServiceFails() {
        String name = "unknown";
        when(configService.findByName(name)).thenThrow(new RuntimeException("Not found"));

        assertThrows(RuntimeException.class, () -> configFacade.findByName(name));

        verify(configService).findByName(name);
        verifyNoInteractions(configMapper);
    }
}