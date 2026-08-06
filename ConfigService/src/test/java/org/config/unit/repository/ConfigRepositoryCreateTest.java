package org.config.unit.repository;

import org.config.data.model.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigRepositoryCreateTest extends BaseConfigRepositoryTest {

    @Test
    void save_ShouldPersistConfig() {
        Config config = new Config();
        config.setName("app_setting");
        config.setData("{\"theme\":\"dark\"}");

        Config saved = configRepository.save(config);

        assertNotNull(saved.getId());
        assertEquals("app_setting", saved.getName());

        Config found = entityManager.find(Config.class, saved.getId());
        assertEquals("{\"theme\":\"dark\"}", found.getData());
    }
}