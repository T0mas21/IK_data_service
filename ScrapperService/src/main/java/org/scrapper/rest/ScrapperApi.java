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

    @GetMapping("/scrape")
    public ResponseEntity<Object> scrape(@Valid ScrapeRequestDto requestDto) {
        return ResponseEntity.ok(scrapperService.scrape(
                requestDto.url(),
                requestDto.timeoutSeconds(),
                requestDto.userAgent(),
                requestDto.strategy()
        ));
    }

    @GetMapping("/strategies")
    public Map<String, List<ScrapperStrategy>> getAllStrategies() {
        return Map.of("strategies", List.of(ScrapperStrategy.values()));
    }
}