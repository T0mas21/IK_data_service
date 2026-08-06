package org.scrapper.rest;

import jakarta.validation.Valid;
import org.scrapper.dto.ScrapeRequestDto;
import org.scrapper.service.ScrapperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scrapper_api/scrape")
public class ScrapperApi {

    private final ScrapperService scrapperService;

    @Autowired
    public ScrapperApi(ScrapperService scrapperService) {
        this.scrapperService = scrapperService;
    }

    @GetMapping
    public ResponseEntity<Object> scrape(@Valid ScrapeRequestDto requestDto) {
        return ResponseEntity.ok(scrapperService.scrape(
                requestDto.url(),
                requestDto.timeoutSeconds(),
                requestDto.userAgent(),
                requestDto.strategy()
        ));
    }
}