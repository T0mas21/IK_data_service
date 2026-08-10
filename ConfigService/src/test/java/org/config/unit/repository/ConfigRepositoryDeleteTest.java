package org.config.unit.repository;

import org.config.data.model.Config;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
class ConfigRepositoryDeleteTest extends BaseConfigRepositoryTest {

    @Test
    void deleteByName_WhenExists_ShouldRemoveRecord() {
        Config config = new Config();
        config.setName("to_delete");
        config.setDescription("Konfigurace ke smazání");
        config.setTimeout(2000);
        config.setUserAgent("Mozilla/5.0");
        config.setUrl("https://example.com");

        entityManager.persistAndFlush(config);

        configRepository.deleteByName("to_delete");
        entityManager.flush();

        Optional<Config> result = configRepository.findByName("to_delete");
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteByName_WhenNotExists_ShouldNotFail() {
        assertDoesNotThrow(() -> configRepository.deleteByName("non_existing_config"));
    }
}