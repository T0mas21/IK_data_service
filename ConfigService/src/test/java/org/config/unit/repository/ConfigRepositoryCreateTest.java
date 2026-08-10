package org.config.unit.repository;

import org.config.data.model.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigRepositoryCreateTest extends BaseConfigRepositoryTest {

    @Test
    void save_ShouldPersistConfig() {
        Config config = new Config();
        config.setName("app_setting");
        config.setDescription("Aplikacni nastaveni");
        config.setTimeout(5000);
        config.setUserAgent("Mozilla/5.0");
        config.setUrl("https://example.com");

        Config saved = configRepository.save(config);

        assertNotNull(saved.getId());
        assertEquals("app_setting", saved.getName());

        Config found = entityManager.find(Config.class, saved.getId());
        assertNotNull(found);
        assertEquals("Aplikacni nastaveni", found.getDescription());
        assertEquals(5000, found.getTimeout());
        assertEquals("Mozilla/5.0", found.getUserAgent());
        assertEquals("https://example.com", found.getUrl());
    }
}