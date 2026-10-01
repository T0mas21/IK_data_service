package org.config.mappers.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.config.data.model.Config;
import org.config.dto.ConfigDto;
import org.config.mappers.FileMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigMapperImplTest {

    private final FileMapper fileMapper = new FileMapperImpl();
    private final ConfigMapperImpl mapper = new ConfigMapperImpl(fileMapper, new ObjectMapper());

    @Test
    void toEntity_SerializesTablesToJsonString() {
        List<Map<String, Object>> tables = List.of(
                Map.of("table", Map.of("columns", List.of(Map.of("1", "Název")), "row", List.of()))
        );
        ConfigDto dto = new ConfigDto("cfg", null, null, null, null, null, null, tables, List.of());

        Config entity = mapper.toEntity(dto);

        assertEquals(tables, readBack(entity.getTables()));
    }

    @Test
    void toEntity_TablesNull_ProducesNullOnEntity() {
        ConfigDto dto = new ConfigDto("cfg", null, null, null, null, null, null, null, List.of());

        Config entity = mapper.toEntity(dto);

        assertNull(entity.getTables());
    }

    @Test
    void toEntity_CopiesWebTextDirectly() {
        ConfigDto dto = new ConfigDto("cfg", null, null, null, null, null, "text ze stranky", null, List.of());

        Config entity = mapper.toEntity(dto);

        assertEquals("text ze stranky", entity.getWebText());
    }

    @Test
    void toDto_DeserializesTablesFromJsonString() {
        Config entity = new Config();
        entity.setName("cfg");
        entity.setTables("[{\"table\":{\"columns\":[{\"1\":\"Název\"}],\"row\":[]}}]");

        ConfigDto dto = mapper.toDto(entity);

        assertEquals(
                List.of(Map.of("table", Map.of("columns", List.of(Map.of("1", "Název")), "row", List.of()))),
                dto.tables()
        );
    }

    @Test
    void toDto_TablesNull_ProducesNullOnDto() {
        Config entity = new Config();
        entity.setName("cfg");

        ConfigDto dto = mapper.toDto(entity);

        assertNull(dto.tables());
    }

    @Test
    void roundTrip_PreservesTablesAndWebText() {
        List<Map<String, Object>> tables = List.of(
                Map.of("table", Map.of("columns", List.of(Map.of("1", "Sloupec")), "row", List.of(Map.of("Sloupec", "hodnota"))))
        );
        ConfigDto dto = new ConfigDto("cfg", "popis", 10, "agent", "https://example.com",
                "vlastni", "text ze stranky", tables, List.of());

        ConfigDto roundTripped = mapper.toDto(mapper.toEntity(dto));

        assertEquals("text ze stranky", roundTripped.webText());
        assertEquals(tables, roundTripped.tables());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readBack(String json) {
        try {
            return new ObjectMapper().readValue(json, List.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
