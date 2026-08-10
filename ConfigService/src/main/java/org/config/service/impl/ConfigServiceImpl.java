package org.config.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.config.data.model.Config;
import org.config.data.repository.ConfigRepository;
import org.config.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class ConfigServiceImpl implements ConfigService {

    private final ConfigRepository configRepository;

    @Autowired
    public ConfigServiceImpl(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    public Config createConfig(Config newConfig) {
        if (newConfig.getName() == null || newConfig.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Jméno konfiguračního souboru nesmí být prázdné."
            );
        }

        if (configRepository.findByName(newConfig.getName()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Konfigurační soubor '" + newConfig.getName() + "' již existuje."
            );
        }

        if (newConfig.getTimeout() != null && newConfig.getTimeout() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Timeout nesmí být záporná hodnota."
            );
        }

        return this.configRepository.save(newConfig);
    }

    @Override
    public Config findByName(String name) {
        return this.configRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Konfigurace '" + name + "' nebyla nalezena."
                ));
    }

    @Override
    public List<String> findAllNames() {
        return this.configRepository.findAllNames();
    }

    @Override
    public List<Config> findAll() {
        return this.configRepository.findAll();
    }

    @Override
    @Transactional
    public Config updateConfig(String name, Config updatedConfig) {
        Config existingConfig = configRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Konfigurační soubor '" + name + "' neexistuje."
                ));

        if (updatedConfig.getTimeout() != null && updatedConfig.getTimeout() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Timeout nesmí být záporná hodnota."
            );
        }

        existingConfig.setDescription(updatedConfig.getDescription());
        existingConfig.setTimeout(updatedConfig.getTimeout());
        existingConfig.setUserAgent(updatedConfig.getUserAgent());
        existingConfig.setUrl(updatedConfig.getUrl());

        return existingConfig;
    }

    @Override
    @Transactional
    public void deleteByName(String name) {
        if (configRepository.findByName(name).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Konfigurační soubor '" + name + "' neexistuje."
            );
        }
        this.configRepository.deleteByName(name);
    }
}
