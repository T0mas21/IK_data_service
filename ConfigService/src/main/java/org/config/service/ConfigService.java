package org.config.service;

import org.config.data.model.Config;

import java.util.List;
import java.util.Optional;

public interface ConfigService {

    Config createConfig(Config newConfig);

    Config findByName(String name);

    List<String> findAllNames();

    List<Config> findAll();

    void updateDataByName(String name, String newData);

    void deleteByName(String name);
}
