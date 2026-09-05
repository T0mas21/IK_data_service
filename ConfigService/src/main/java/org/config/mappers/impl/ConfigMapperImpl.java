package org.config.mappers.impl;

import org.config.data.model.Config;
import org.config.data.model.File;
import org.config.dto.ConfigDto;
import org.config.dto.FileDto;
import org.config.mappers.ConfigMapper;
import org.config.mappers.FileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConfigMapperImpl implements ConfigMapper {

    private final FileMapper fileMapper;

    @Autowired
    public ConfigMapperImpl(FileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    @Override
    public ConfigDto toDto(Config configEntity) {
        if (configEntity == null) {
            return null;
        }

        List<FileDto> fileDtos = (configEntity.getFiles() != null)
                ? configEntity.getFiles().stream().map(fileMapper::toDto).toList()
                : new ArrayList<>();

        return new ConfigDto(
                configEntity.getName(),
                configEntity.getDescription(),
                configEntity.getTimeout(),
                configEntity.getUserAgent(),
                configEntity.getUrl(),
                configEntity.getContent(),
                fileDtos
        );
    }

    @Override
    public Config toEntity(ConfigDto configDto) {
        if (configDto == null) {
            return null;
        }

        Config config = new Config();
        config.setName(configDto.name());
        config.setDescription(configDto.description());
        config.setTimeout(configDto.timeout());
        config.setUserAgent(configDto.userAgent());
        config.setUrl(configDto.url());
        config.setContent(configDto.content());

        if (configDto.files() != null) {
            for (FileDto fileDto : configDto.files()) {
                File fileEntity = fileMapper.toEntity(fileDto);
                config.addFile(fileEntity);
            }
        }

        return config;
    }
}