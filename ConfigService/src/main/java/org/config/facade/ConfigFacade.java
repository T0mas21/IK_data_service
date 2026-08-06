package org.config.facade;

import org.config.dto.ConfigDto;

import java.util.List;

public interface ConfigFacade {
    ConfigDto createConfig(ConfigDto configDto);
    ConfigDto findByName(String name);
    List<String> findAllNames();
    List<ConfigDto> findAll();
    void updateDataByName(String name, String newData);
    void deleteByName(String name);
}
