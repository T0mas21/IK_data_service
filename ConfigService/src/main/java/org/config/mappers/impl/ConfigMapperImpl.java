package org.config.mappers.impl;

import org.config.data.model.Config;
import org.config.dto.ConfigDto;
import org.config.mappers.ConfigMapper;
import org.springframework.stereotype.Component;

@Component
public class ConfigMapperImpl implements ConfigMapper {

    @Override
    public ConfigDto toDto(Config configEntity) {
        if (configEntity == null) {
            return null;
        }

        return new ConfigDto(
                configEntity.getName(),
                configEntity.getDescription(),
                configEntity.getTimeout(),
                configEntity.getUserAgent(),
                configEntity.getUrl()
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

        return config;
    }
}
