package org.config.rest;

import jakarta.validation.Valid;
import org.config.data.model.Config;
import org.config.dto.ConfigDto;
import org.config.dto.ConfigNamesDto;
import org.config.dto.FileDto;
import org.config.dto.RegisterFileDto;
import org.config.dto.UploadUrlDto;
import org.config.dto.UploadUrlRequestDto;
import org.config.facade.ConfigFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/scrapper_api/config")
public class ConfigApi {

    private final ConfigFacade configFacade;

    @Autowired
    public ConfigApi(ConfigFacade configFacade) {
        this.configFacade = configFacade;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConfigDto> createConfig(@Valid @RequestBody ConfigDto configDto) {
        ConfigDto created = configFacade.createConfig(configDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConfigDto> createConfigWithFiles(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer timeout,
            @RequestParam(required = false) String userAgent,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) String customText,
            @RequestParam(required = false) List<MultipartFile> files) {
        ConfigDto configDto = new ConfigDto(name, description, timeout, userAgent, url, customText, List.of());
        ConfigDto created = configFacade.createConfig(configDto, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{name}")
    public ResponseEntity<ConfigDto> getByName(@PathVariable String name) {
        ConfigDto config = configFacade.findByName(name);
        return ResponseEntity.ok(config);
    }

    @GetMapping("/names")
    public ResponseEntity<ConfigNamesDto> getAllNames() {
        return ResponseEntity.ok(configFacade.findAllNames());
    }

    @GetMapping
    public ResponseEntity<List<ConfigDto>> getAll() {
        return ResponseEntity.ok(configFacade.findAll());
    }

    @PutMapping("/{name}")
    public ResponseEntity<Void> updateConfig(@PathVariable String name, @RequestBody ConfigDto newData) {
        configFacade.updateConfig(name, newData);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteByName(@PathVariable String name) {
        configFacade.deleteByName(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{configId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileDto> uploadFile(@PathVariable Long configId,
                                               @RequestParam("file") MultipartFile file) {
        FileDto uploaded = configFacade.uploadFile(configId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploaded);
    }

    @DeleteMapping("/{configId}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long configId, @PathVariable Long fileId) {
        configFacade.deleteFile(configId, fileId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Vrátí signed upload URL pro nahrání souboru přímo do Supabase Storage — bajty souboru
     * touto aplikací neprochází. Po úspěšném nahrání zavolej {@link #registerFile}.
     */
    @PostMapping(value = "/{configId}/files/upload-url", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UploadUrlDto> createUploadUrl(@PathVariable Long configId,
                                                          @Valid @RequestBody UploadUrlRequestDto request) {
        return ResponseEntity.ok(configFacade.createUploadUrl(configId, request));
    }

    /**
     * Zaregistruje soubor, který už byl nahrán přímo do Supabase Storage přes signed upload URL
     * (viz {@link #createUploadUrl}).
     */
    @PostMapping(value = "/{configId}/files/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FileDto> registerFile(@PathVariable Long configId,
                                                 @Valid @RequestBody RegisterFileDto request) {
        FileDto registered = configFacade.registerFile(configId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(registered);
    }
}
