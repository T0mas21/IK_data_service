package org.config.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.config.data.model.Config;
import org.config.data.model.File;
import org.config.data.repository.ConfigRepository;
import org.config.data.repository.FileRepository;
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

    @Autowired
    public ConfigServiceImpl(ConfigRepository configRepository, FileRepository fileRepository,
                              SupabaseStorageService supabaseStorageService) {
        this.configRepository = configRepository;
        this.fileRepository = fileRepository;
        this.supabaseStorageService = supabaseStorageService;
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

        return this.configRepository.save(newConfig);
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

        return existingConfig;
    }

    @Override
    @Transactional
    public void deleteByName(String name) {
        if (configRepository.findByName(name).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Konfigurační soubor '" + name + "' neexistuje."
            );
        }
        this.configRepository.deleteByName(name);
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

        return fileRepository.save(file);
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
    }
}
