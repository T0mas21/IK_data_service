package org.scrapper.rest;

import jakarta.validation.Valid;
import org.scrapper.dto.ScrapeRequestDto;
import org.scrapper.enums.ScrapperStrategy;
import org.scrapper.service.ScrapperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/scrapper_api/scrape")
public class ScrapperApi {

    private final ScrapperService scrapperService;

    @Autowired
    public ScrapperApi(ScrapperService scrapperService) {
        this.scrapperService = scrapperService;
    }

    @PostMapping("/scrape")
    public Object scrape(@Valid @RequestBody ScrapeRequestDto dto) {
        return scrapperService.scrape(dto.url(), dto.timeoutSeconds(), dto.userAgent(), dto.strategy());
    }

    @GetMapping("/strategies")
    public Map<String, List<ScrapperStrategy>> getAllStrategies() {
        return Map.of("strategies", List.of(ScrapperStrategy.values()));
    }
}