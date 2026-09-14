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

        validateFilesForCreate(newConfig);

        Config savedConfig = this.configRepository.save(newConfig);
        reindexConfig(savedConfig);
        return savedConfig;
    }

    /**
     * Soubory poslané v {@code files} při vytváření configu musí už být skutečně nahrané
     * v Supabase Storage (viz {@link #createUploadUrlForNewConfig}) — tady se jen ověří, že mají
     * vyplněné povinné údaje, jinak by cascade insert Configu spadl na NOT NULL constraintu
     * v {@code config_files}.
     */
    private void validateFilesForCreate(Config newConfig) {
        if (newConfig.getFiles() == null) {
            return;
        }

        for (File file : newConfig.getFiles()) {
            if (file.getStoragePath() == null || file.getStoragePath().isBlank()
                    || file.getFileName() == null || file.getFileName().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Každý soubor v 'files' musí mít vyplněné 'storagePath' a 'fileName' - "
                                + "soubor se musí nejprve nahrát přes /upload-url endpoint."
                );
            }
        }
    }

    @Override
    public Config findByName(String name) {
        return this.configRepository.findByNameWithFiles(name)
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
        return this.configRepository.findAllWithFiles();
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
        existingConfig.setCustomText(updatedConfig.getCustomText());

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
        String storagePath = buildStoragePath(configId.toString(), originalFileName);

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

        String storagePath = buildStoragePath(configId.toString(), fileName);
        String uploadUrl = supabaseStorageService.createSignedUploadUrl(storagePath);

        return new UploadUrlDto(storagePath, uploadUrl, fileName, fileType);
    }

    @Override
    public UploadUrlDto createUploadUrlForNewConfig(String configName, String fileName, String fileType) {
        if (fileName == null || fileName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jméno souboru nesmí být prázdné.");
        }

        if (configName == null || configName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jméno konfigurace nesmí být prázdné.");
        }

        String storagePath = buildStoragePath(sanitizeForStoragePath(configName), fileName);
        String uploadUrl = supabaseStorageService.createSignedUploadUrl(storagePath);

        return new UploadUrlDto(storagePath, uploadUrl, fileName, fileType);
    }

    private String buildStoragePath(String folder, String fileName) {
        return "configs/" + folder + "/" + UUID.randomUUID() + "_" + fileName;
    }

    /**
     * Config.name je volné uživatelské pole - pro použití jako složka v Supabase Storage se musí
     * omezit na bezpečnou sadu znaků (Supabase cesty nesmí obsahovat mezery, lomítka apod.).
     */
    private String sanitizeForStoragePath(String name) {
        String sanitized = name.trim().toLowerCase().replaceAll("[^a-z0-9_-]+", "_");
        sanitized = sanitized.replaceAll("^_+|_+$", "");
        return sanitized.isBlank() ? UUID.randomUUID().toString() : sanitized;
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

    @Override
    public String getFileDownloadUrl(Long configId, String fileName) {
        File file = fileRepository.findByConfigIdAndFileName(configId, fileName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Soubor '" + fileName + "' nebyl u konfigurace s id " + configId + " nalezen."
                ));

        return supabaseStorageService.createSignedDownloadUrl(file.getStoragePath());
    }

    /**
     * Best-effort přeindexování configu ve Vector službě: naskrapuje aktuální url (pokud je vyplněná)
     * a spolu s custom textem a aktuálním seznamem souborů pošle k zaindexování. Chyby se pouze logují
     * (viz {@link ScrapperServiceClient} a {@link VectorServiceClient}) a nesmí shodit operaci nad configem.
     */
    private void reindexConfig(Config config) {
        String scrapedText = scrapperServiceClient.scrapeText(config.getUrl(), config.getTimeout(), config.getUserAgent());
        vectorServiceClient.indexConfig(config.getId(), config.getCustomText(), scrapedText, config.getFiles());
    }
}
