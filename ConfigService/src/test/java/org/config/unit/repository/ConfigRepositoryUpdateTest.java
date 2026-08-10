package org.config.unit.repository;

import org.config.data.model.Config;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.junit.jupiter.api.Assertions.*;

class ConfigRepositoryUpdateTest extends BaseConfigRepositoryTest {

    @Test
    void update_WhenExists_ShouldUpdateFieldsCorrectly() {
        Config config = new Config();
        config.setName("to_update");
        config.setDescription("Původní popis");
        config.setTimeout(1000);
        config.setUserAgent("OldAgent");
        config.setUrl("https://old-url.com");

        entityManager.persistAndFlush(config);
        entityManager.clear();

        Config existingConfig = configRepository.findByName("to_update").orElseThrow();
        existingConfig.setDescription("Nový popis");
        existingConfig.setTimeout(5000);
        existingConfig.setUserAgent("NewAgent");
        existingConfig.setUrl("https://new-url.com");

        configRepository.save(existingConfig);
        entityManager.flush();
        entityManager.clear();

        Config updated = configRepository.findByName("to_update").orElseThrow();

        assertEquals("Nový popis", updated.getDescription());
        assertEquals(5000, updated.getTimeout());
        assertEquals("NewAgent", updated.getUserAgent());
        assertEquals("https://new-url.com", updated.getUrl());
    }

    @Test
    void findByName_WhenNotExists_ShouldReturnEmptyOptional() {
        assertTrue(configRepository.findByName("non_existing").isEmpty());
    }
}
