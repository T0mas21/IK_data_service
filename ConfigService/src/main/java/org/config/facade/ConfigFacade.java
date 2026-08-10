package org.config.facade;

import org.config.data.model.Config;
import org.config.dto.ConfigDto;

import java.util.List;

public interface ConfigFacade {
    ConfigDto createConfig(ConfigDto configDto);
    ConfigDto findByName(String name);
    List<String> findAllNames();
    List<ConfigDto> findAll();
    void updateConfig(String name, ConfigDto newConfig);
    void deleteByName(String name);
}
