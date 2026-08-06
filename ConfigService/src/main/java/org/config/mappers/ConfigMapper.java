package org.config.mappers;

import org.config.data.model.Config;
import org.config.dto.ConfigDto;

public interface ConfigMapper {
    ConfigDto toDto(Config configEntity);
    Config toEntity(ConfigDto configDto);
}
