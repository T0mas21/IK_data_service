package org.config.mappers.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.config.data.model.Config;
import org.config.dto.ConfigDto;
import org.config.dto.FileDto;
import org.config.mappers.ConfigMapper;
import org.config.mappers.FileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ConfigMapperImpl implements ConfigMapper {

    private final FileMapper fileMapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public ConfigMapperImpl(FileMapper fileMapper, ObjectMapper objectMapper) {
        this.fileMapper = fileMapper;
        this.objectMapper = objectMapper;
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
                configEntity.getCustomText(),
                configEntity.getWebText(),
                deserializeTables(configEntity.getTables()),
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
        config.setCustomText(configDto.customText());
        config.setWebText(configDto.webText());
        config.setTables(serializeTables(configDto.tables()));

        return config;
    }

    private String serializeTables(List<Map<String, Object>> tables) {
        if (tables == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(tables);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Tabulky z webu se nepodařilo serializovat do JSON.", e);
        }
    }

    private List<Map<String, Object>> deserializeTables(String tablesJson) {
        if (tablesJson == null || tablesJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(tablesJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Tabulky z webu uložené u konfigurace se nepodařilo načíst z JSON.", e);
        }
    }
}