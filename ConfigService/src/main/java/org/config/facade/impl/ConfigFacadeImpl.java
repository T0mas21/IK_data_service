package org.config.facade.impl;

import org.config.data.model.Config;
import org.config.data.model.File;
import org.config.dto.ConfigDto;
import org.config.dto.ConfigNameItemDto;
import org.config.dto.ConfigNamesDto;
import org.config.dto.FileDto;
import org.config.dto.RegisterFileDto;
import org.config.dto.UploadUrlByNameRequestDto;
import org.config.dto.UploadUrlDto;
import org.config.dto.UploadUrlRequestDto;
import org.config.facade.ConfigFacade;
import org.config.mappers.ConfigMapper;
import org.config.mappers.FileMapper;
import org.config.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class ConfigFacadeImpl implements ConfigFacade {

    private final ConfigService configService;
    private final ConfigMapper configMapper;
    private final FileMapper fileMapper;

    @Autowired
    public ConfigFacadeImpl(ConfigService configService, ConfigMapper configMapper, FileMapper fileMapper) {
        this.configService = configService;
        this.configMapper = configMapper;
        this.fileMapper = fileMapper;
    }

    @Override
    public ConfigDto createConfig(ConfigDto configDto) {
        Config entity = configMapper.toEntity(configDto);
        Config savedEntity = configService.createConfig(entity, configDto.files());
        return configMapper.toDto(savedEntity);
    }

    @Override
    public ConfigDto createConfig(ConfigDto configDto, List<MultipartFile> files) {
        Config entity = configMapper.toEntity(configDto);
        Config savedEntity = configService.createConfig(entity, configDto.files());

        List<FileDto> uploadedFiles = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    File savedFile = configService.addFileToConfig(savedEntity.getId(), file);
                    uploadedFiles.add(fileMapper.toDto(savedFile));
                }
            }
        }

        ConfigDto createdDto = configMapper.toDto(savedEntity);
        return new ConfigDto(
                createdDto.name(),
                createdDto.description(),
                createdDto.timeout(),
                createdDto.userAgent(),
                createdDto.url(),
                createdDto.customText(),
                createdDto.webText(),
                createdDto.tables(),
                uploadedFiles
        );
    }

    @Override
    public ConfigDto findByName(String name) {
        Config entity = configService.findByName(name);
        return withFileContents(configMapper.toDto(entity));
    }

    /**
     * Doplní do souborů configu jejich base64 obsah stažený ze Supabase Storage - mapper sám vrací
     * {@code content == null}, protože DB drží jen metadata. Soubor, který se nepodaří stáhnout,
     * zůstane bez obsahu (best-effort, viz {@link ConfigService#downloadFileContent}), aby jeden
     * nedostupný soubor neshodil čtení celého configu.
     */
    private ConfigDto withFileContents(ConfigDto configDto) {
        if (configDto == null || configDto.files() == null || configDto.files().isEmpty()) {
            return configDto;
        }

        List<FileDto> filesWithContent = configDto.files().stream()
                .map(file -> {
                    byte[] content = configService.downloadFileContent(file.storagePath());
                    return new FileDto(
                            file.id(),
                            file.fileName(),
                            file.storagePath(),
                            file.fileType(),
                            content != null ? Base64.getEncoder().encodeToString(content) : null,
                            null
                    );
                })
                .toList();

        return new ConfigDto(
                configDto.name(),
                configDto.description(),
                configDto.timeout(),
                configDto.userAgent(),
                configDto.url(),
                configDto.customText(),
                configDto.webText(),
                configDto.tables(),
                filesWithContent
        );
    }

    @Override
    public ConfigNamesDto findAllNames() {
        List<ConfigNameItemDto> items = configService.findAllNames()
                .stream()
                .map(ConfigNameItemDto::new)
                .toList();
        return new ConfigNamesDto(items);
    }

    @Override
    public List<ConfigDto> findAll() {
        List<Config> entities = configService.findAll();
        return entities.stream()
                .map(configMapper::toDto)
                .toList();
    }

    @Override
    public void updateConfig(String name, ConfigDto newConfig) {
        Config newConfigEntity = configMapper.toEntity(newConfig);
        configService.updateConfig(name, newConfigEntity, newConfig.files());
    }

    @Override
    public void deleteByName(String name) {
        configService.deleteByName(name);
    }


    @Override
    public FileDto uploadFile(Long configId, MultipartFile multipartFile) {
        File savedFile = configService.addFileToConfig(configId, multipartFile);
        return fileMapper.toDto(savedFile);
    }

    @Override
    public void deleteFile(Long configId, Long fileId) {
        configService.removeFileFromConfig(configId, fileId);
    }

    @Override
    public UploadUrlDto createUploadUrl(Long configId, UploadUrlRequestDto request) {
        return configService.createUploadUrl(configId, request.fileName(), request.fileType());
    }

    @Override
    public UploadUrlDto createUploadUrlForNewConfig(UploadUrlByNameRequestDto request) {
        return configService.createUploadUrlForNewConfig(request.configName(), request.fileName(), request.fileType());
    }

    @Override
    public FileDto registerFile(Long configId, RegisterFileDto request) {
        File savedFile = configService.registerFile(configId, request.storagePath(), request.fileName(), request.fileType());
        return fileMapper.toDto(savedFile);
    }

    @Override
    public String getFileDownloadUrl(Long configId, String fileName) {
        return configService.getFileDownloadUrl(configId, fileName);
    }
}