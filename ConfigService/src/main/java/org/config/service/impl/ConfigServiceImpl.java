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

    @Autowired
    private ConfigRepository configRepository;
    @Autowired
    private ObjectMapper objectMapper;

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

        if (!isValidJson(newConfig.getData())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Pole 'data' neobsahuje validní JSON formát."
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
    public List<String> findAllNames(){
        return this.configRepository.findAllNames();
    }

    @Override
    public List<Config> findAll(){
        return this.configRepository.findAll();
    }

    @Override
    @Transactional
    public void updateDataByName(String name, String newData) {
        if (!isValidJson(newData)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nová data neobsahují validní JSON.");
        }

        Config config = configRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Konfigurační soubor neexistuje."));

        config.setData(newData);
    }

    @Override
    public void deleteByName(String name){
        this.configRepository.deleteByName(name);
    }

    private boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.isBlank()) {
            return false;
        }
        try {
            objectMapper.readTree(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
