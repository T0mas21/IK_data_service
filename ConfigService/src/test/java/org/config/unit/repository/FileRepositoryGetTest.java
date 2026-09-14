package org.config.unit.repository;

import org.config.data.model.Config;
import org.config.data.model.File;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FileRepositoryGetTest extends BaseFileRepositoryTest {

    private Config persistConfig(String name) {
        Config config = new Config();
        config.setName(name);
        return entityManager.persistAndFlush(config);
    }

    @Test
    void findByConfigIdAndFileName_WhenExists_ShouldReturnFile() {
        Config config = persistConfig("config_a");
        File file = new File(config, "configs/1/uuid_smlouva.pdf", "smlouva.pdf", "application/pdf");
        entityManager.persistAndFlush(file);

        Optional<File> result = fileRepository.findByConfigIdAndFileName(config.getId(), "smlouva.pdf");

        assertTrue(result.isPresent());
        assertEquals("configs/1/uuid_smlouva.pdf", result.get().getStoragePath());
    }

    @Test
    void findByConfigIdAndFileName_WhenNotExists_ShouldReturnEmpty() {
        Config config = persistConfig("config_b");

        Optional<File> result = fileRepository.findByConfigIdAndFileName(config.getId(), "neexistuje.pdf");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByConfigIdAndFileName_SameFileNameInDifferentConfig_ShouldNotBeAmbiguous() {
        Config configA = persistConfig("config_c");
        Config configB = persistConfig("config_d");

        File fileA = new File(configA, "configs/" + configA.getId() + "/uuid_a_data.json", "data.json", "application/json");
        File fileB = new File(configB, "configs/" + configB.getId() + "/uuid_b_data.json", "data.json", "application/json");
        entityManager.persistAndFlush(fileA);
        entityManager.persistAndFlush(fileB);

        Optional<File> resultA = fileRepository.findByConfigIdAndFileName(configA.getId(), "data.json");
        Optional<File> resultB = fileRepository.findByConfigIdAndFileName(configB.getId(), "data.json");

        assertTrue(resultA.isPresent());
        assertTrue(resultB.isPresent());
        assertEquals(fileA.getStoragePath(), resultA.get().getStoragePath());
        assertEquals(fileB.getStoragePath(), resultB.get().getStoragePath());
    }

    @Test
    void save_DuplicateFileNameInSameConfig_ShouldViolateUniqueConstraint() {
        Config config = persistConfig("config_e");
        File first = new File(config, "configs/1/uuid_1_data.json", "data.json", "application/json");
        entityManager.persistAndFlush(first);

        File duplicate = new File(config, "configs/1/uuid_2_data.json", "data.json", "application/json");

        assertThrows(ConstraintViolationException.class,
                () -> entityManager.persistAndFlush(duplicate));
    }
}
