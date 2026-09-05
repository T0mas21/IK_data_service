package org.config.facade;

import org.config.dto.ConfigDto;
import org.config.dto.ConfigNamesDto;
import org.config.dto.FileDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ConfigFacade {
    ConfigDto createConfig(ConfigDto configDto);
    ConfigDto findByName(String name);
    ConfigNamesDto findAllNames();
    List<ConfigDto> findAll();
    void updateConfig(String name, ConfigDto newConfig);
    void deleteByName(String name);

    FileDto uploadFile(Long configId, MultipartFile file);
    void deleteFile(Long configId, Long fileId);
}