package org.config.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.config.client.ScrapperServiceClient;
import org.config.client.VectorServiceClient;
import org.config.data.model.Config;
import org.config.data.model.File;
import org.config.data.repository.ConfigRepository;
import org.config.data.repository.FileRepository;
import org.config.dto.UploadUrlDto;
import org.config.service.ConfigService;
import org.config.service.storage.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConfigServiceImpl implements ConfigService {

    private final ConfigRepository configRepository;
    private final FileRepository fileRepository;
    private final SupabaseStorageService supabaseStorageService;
    private final ScrapperServiceClient scrapperServiceClient;
    private final VectorServiceClient vectorServiceClient;

    @Autowired
    public ConfigServiceImpl(ConfigRepository configRepository, FileRepository fileRepository,
                              SupabaseStorageService supabaseStorageService,
                              ScrapperServiceClient scrapperServiceClient,
                              VectorServiceClient vectorServiceClient) {
        this.configRepository = configRepository;
        this.fileRepository = fileRepository;
        this.supabaseStorageService = supabaseStorageService;
        this.scrapperServiceClient = scrapperServiceClient;
        this.vectorServiceClient = vectorServiceClient;
    }

    @Override
    public Config createConfig(Config newConfig) {
        if (newConfig.getName() == null || newConfig.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Jméno konfiguračního souboru nesmí být prázdné."
            );
        }

        if (configRepository.findByName(newConfig.getName()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Konfigurační soubor '" + newConfig.getName() + "' již existuje."
            );
        }

        if (newConfig.getTimeout() != null && newConfig.getTimeout() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Timeout nesmí být záporná hodnota."
            );
        }

        Config savedConfig = this.configRepository.save(newConfig);
        reindexConfig(savedConfig);
        return savedConfig;
    }

    @Override
    public Config findByName(String name) {
        return this.configRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Konfigurace '" + name + "' nebyla nalezena."
                ));
    }

    @Override
    public List<String> findAllNames() {
        return this.configRepository.findAllNames();
    }

    @Override
    public List<Config> findAll() {
        return this.configRepository.findAll();
    }

    @Override
    @Transactional
    public Config updateConfig(String name, Config updatedConfig) {
        Config existingConfig = configRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Konfigurační soubor '" + name + "' neexistuje."
                ));

        if (updatedConfig.getTimeout() != null && updatedConfig.getTimeout() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Timeout nesmí být záporná hodnota."
            );
        }

        existingConfig.setDescription(updatedConfig.getDescription());
        existingConfig.setTimeout(updatedConfig.getTimeout());
        existingConfig.setUserAgent(updatedConfig.getUserAgent());
        existingConfig.setUrl(updatedConfig.getUrl());
        existingConfig.setContent(updatedConfig.getContent());

        reindexConfig(existingConfig);
        return existingConfig;
    }

    @Override
    @Transactional
    public void deleteByName(String name) {
        Config existingConfig = configRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Konfigurační soubor '" + name + "' neexistuje."
                ));
        this.configRepository.deleteByName(name);
        vectorServiceClient.deleteConfig(existingConfig.getId());
    }

    @Override
    @Transactional
    public File addFileToConfig(Long configId, MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nahrávaný soubor nesmí být prázdný.");
        }

        Config config = configRepository.findByIdWithFiles(configId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Konfigurace s id " + configId + " nebyla nalezena."
                ));

        String originalFileName = multipartFile.getOriginalFilename();
        String storagePath = "configs/" + configId + "/" + UUID.randomUUID() + "_" + originalFileName;

        byte[] content;
        try {
            content = multipartFile.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nahrávaný soubor se nepodařilo přečíst.", e);
        }

        supabaseStorageService.uploadFile(storagePath, content, multipartFile.getContentType());

        File file = new File(config, storagePath, originalFileName, multipartFile.getContentType());
        config.addFile(file);

        File savedFile = fileRepository.save(file);
        reindexConfig(config);
        return savedFile;
    }

    @Override
    @Transactional
    public void removeFileFromConfig(Long configId, Long fileId) {
        Config config = configRepository.findByIdWithFiles(configId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Konfigurace s id " + configId + " nebyla nalezena."
                ));

        File file = config.getFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Soubor s id " + fileId + " nebyl u této konfigurace nalezen."
                ));

        supabaseStorageService.deleteFile(file.getStoragePath());

        config.removeFile(file);
        fileRepository.delete(file);
        reindexConfig(config);
    }

    @Override
    public UploadUrlDto createUploadUrl(Long configId, String fileName, String fileType) {
        if (fileName == null || fileName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jméno souboru nesmí být prázdné.");
        }

        if (configRepository.findById(configId).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Konfigurace s id " + configId + " nebyla nalezena."
            );
        }

        String storagePath = "configs/" + configId + "/" + UUID.randomUUID() + "_" + fileName;
        String uploadUrl = supabaseStorageService.createSignedUploadUrl(storagePath);

        return new UploadUrlDto(storagePath, uploadUrl, fileName, fileType);
    }

    @Override
    @Transactional
    public File registerFile(Long configId, String storagePath, String fileName, String fileType) {
        Config config = configRepository.findByIdWithFiles(configId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Konfigurace s id " + configId + " nebyla nalezena."
                ));

        File file = new File(config, storagePath, fileName, fileType);
        config.addFile(file);

        File savedFile = fileRepository.save(file);
        reindexConfig(config);
        return savedFile;
    }

    /**
     * Best-effort přeindexování configu ve Vector službě: naskrapuje aktuální url (pokud je vyplněná)
     * a spolu s custom textem a aktuálním seznamem souborů pošle k zaindexování. Chyby se pouze logují
     * (viz {@link ScrapperServiceClient} a {@link VectorServiceClient}) a nesmí shodit operaci nad configem.
     */
    private void reindexConfig(Config config) {
        String scrapedText = scrapperServiceClient.scrapeText(config.getUrl(), config.getTimeout(), config.getUserAgent());
        vectorServiceClient.indexConfig(config.getId(), config.getContent(), scrapedText, config.getFiles());
    }
}
