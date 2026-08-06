package org.config.unit.repository;

import org.config.data.model.Config;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConfigRepositoryGetTest extends BaseConfigRepositoryTest {

    @Test
    void findByName_WhenExists_ShouldReturnConfig() {
        Config config = new Config();
        config.setName("find_me");
        config.setData("{}");
        entityManager.persistAndFlush(config);

        Optional<Config> result = configRepository.findByName("find_me");

        assertTrue(result.isPresent());
        assertEquals("find_me", result.get().getName());
    }

    @Test
    void findByName_WhenNotExists_ShouldReturnEmpty() {
        Optional<Config> result = configRepository.findByName("non_existing");

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllNames_ShouldReturnOnlyNamesList() {
        Config c1 = new Config();
        c1.setName("config_a");
        c1.setData("{}");

        Config c2 = new Config();
        c2.setName("config_b");
        c2.setData("{}");

        entityManager.persist(c1);
        entityManager.persist(c2);
        entityManager.flush();

        List<String> names = configRepository.findAllNames();

        assertTrue(names.contains("config_a"));
        assertTrue(names.contains("config_b"));
    }
}
