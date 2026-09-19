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
                uploadedFiles
        );
    }

    @Override
    public ConfigDto findByName(String name) {
        Config entity = configService.findByName(name);
        return configMapper.toDto(entity);
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