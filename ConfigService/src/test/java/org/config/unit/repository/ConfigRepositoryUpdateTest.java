package org.config.unit.repository;

import org.config.data.model.Config;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.junit.jupiter.api.Assertions.*;

class ConfigRepositoryUpdateTest extends BaseConfigRepositoryTest {

    @Test
    void updateDataByName_WhenExists_ShouldUpdateDataAndReturnUpdatedRowsCount() throws Exception {
        Config config = new Config();
        config.setName("to_update");
        config.setData("{\"version\":1}");
        entityManager.persistAndFlush(config);
        entityManager.clear();

        int updatedCount = configRepository.updateDataByName("to_update", "{\"version\":2}");

        assertEquals(1, updatedCount);

        Config updated = configRepository.findByName("to_update").orElseThrow();
        JSONAssert.assertEquals("{\"version\":2}", updated.getData(), JSONCompareMode.LENIENT);
    }

    @Test
    void updateDataByName_WhenNotExists_ShouldReturnZero() {
        int updatedCount = configRepository.updateDataByName("non_existing", "{\"version\":2}");

        assertEquals(0, updatedCount);
    }
}
