package org.config.facade.impl;

import org.config.data.model.Config;
import org.config.dto.ConfigDto;
import org.config.facade.ConfigFacade;
import org.config.mappers.ConfigMapper;
import org.config.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConfigFacadeImpl implements ConfigFacade {

    private final ConfigService configService;
    private final ConfigMapper configMapper;

    @Autowired
    public ConfigFacadeImpl(ConfigService configService, ConfigMapper configMapper) {
        this.configService = configService;
        this.configMapper = configMapper;
    }

    @Override
    public ConfigDto createConfig(ConfigDto configDto) {
        Config entity = configMapper.toEntity(configDto);
        Config savedEntity = configService.createConfig(entity);
        return configMapper.toDto(savedEntity);
    }

    @Override
    public ConfigDto findByName(String name) {
        Config entity = configService.findByName(name);
        return configMapper.toDto(entity);
    }

    @Override
    public List<String> findAllNames() {
        return configService.findAllNames();
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
        configService.updateConfig(name, newConfigEntity);
    }

    @Override
    public void deleteByName(String name) {
        configService.deleteByName(name);
    }
}
