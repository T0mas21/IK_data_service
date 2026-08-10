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
        config.setDescription("Testovací popis");
        config.setTimeout(3000);
        config.setUserAgent("Mozilla/5.0");
        config.setUrl("https://example.com");

        entityManager.persistAndFlush(config);

        Optional<Config> result = configRepository.findByName("find_me");

        assertTrue(result.isPresent());
        Config found = result.get();
        assertEquals("find_me", found.getName());
        assertEquals("Testovací popis", found.getDescription());
        assertEquals(3000, found.getTimeout());
        assertEquals("Mozilla/5.0", found.getUserAgent());
        assertEquals("https://example.com", found.getUrl());
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
        c1.setDescription("Popis A");
        c1.setTimeout(1000);
        c1.setUserAgent("AgentA");
        c1.setUrl("https://a.com");

        Config c2 = new Config();
        c2.setName("config_b");
        c2.setDescription("Popis B");
        c2.setTimeout(2000);
        c2.setUserAgent("AgentB");
        c2.setUrl("https://b.com");

        entityManager.persist(c1);
        entityManager.persist(c2);
        entityManager.flush();

        List<String> names = configRepository.findAllNames();

        assertTrue(names.contains("config_a"));
        assertTrue(names.contains("config_b"));
    }
}