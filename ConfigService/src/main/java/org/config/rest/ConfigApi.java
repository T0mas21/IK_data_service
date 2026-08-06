package org.config.rest;

import jakarta.validation.Valid;
import org.config.dto.ConfigDto;
import org.config.facade.ConfigFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/scrapper_api/config")
public class ConfigApi {

    private final ConfigFacade configFacade;

    @Autowired
    public ConfigApi(ConfigFacade configFacade) {
        this.configFacade = configFacade;
    }

    @PostMapping
    public ResponseEntity<ConfigDto> createConfig(@Valid @RequestBody ConfigDto configDto) {
        ConfigDto created = configFacade.createConfig(configDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{name}")
    public ResponseEntity<ConfigDto> getByName(@PathVariable String name) {
        ConfigDto config = configFacade.findByName(name);
        return ResponseEntity.ok(config);
    }

    @GetMapping("/names")
    public ResponseEntity<List<String>> getAllNames() {
        return ResponseEntity.ok(configFacade.findAllNames());
    }

    @GetMapping
    public ResponseEntity<List<ConfigDto>> getAll() {
        return ResponseEntity.ok(configFacade.findAll());
    }

    @PutMapping("/{name}")
    public ResponseEntity<Void> updateDataByName(@PathVariable String name, @RequestBody String newData) {
        configFacade.updateDataByName(name, newData);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteByName(@PathVariable String name) {
        configFacade.deleteByName(name);
        return ResponseEntity.noContent().build();
    }
}
