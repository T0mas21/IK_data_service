package org.config.mappers;

import org.config.dto.FileDto;
import org.config.data.model.File;

public interface FileMapper {
    FileDto toDto(File fileEntity);
    File toEntity(FileDto fileDto);
}
