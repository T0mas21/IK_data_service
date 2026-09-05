package org.config.mappers.impl;

import org.config.data.model.File;
import org.config.dto.FileDto;
import org.config.mappers.FileMapper;
import org.springframework.stereotype.Component;

@Component
public class FileMapperImpl implements FileMapper {

    @Override
    public FileDto toDto(File fileEntity) {
        if (fileEntity == null) {
            return null;
        }

        return new FileDto(
                fileEntity.getId(),
                fileEntity.getFileName(),
                fileEntity.getStoragePath(),
                fileEntity.getFileType()
        );
    }

    @Override
    public File toEntity(FileDto fileDto) {
        if (fileDto == null) {
            return null;
        }

        File file = new File();
        file.setId(fileDto.id());
        file.setFileName(fileDto.fileName());
        file.setStoragePath(fileDto.storagePath());
        file.setFileType(fileDto.fileType());

        return file;
    }
}