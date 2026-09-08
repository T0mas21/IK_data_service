package org.config.client.impl;

import org.config.client.ScrapperServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ScrapperServiceClientImpl implements ScrapperServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ScrapperServiceClientImpl.class);

    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (compatible; ScrapperAppVectorIndexer/1.0)";

    private final RestTemplate restTemplate;

    @Value("${scrapper-service.url}")
    private String scrapperServiceUrl;

    public ScrapperServiceClientImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public String scrapeText(String url, Integer timeoutSeconds, String userAgent) {
        if (url == null || url.isBlank()) {
            return null;
        }

        Map<String, Object> requestBody = Map.of(
                "url", url,
                "timeoutSeconds", timeoutSeconds != null && timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS,
                "userAgent", userAgent != null && !userAgent.isBlank() ? userAgent : DEFAULT_USER_AGENT,
                "strategy", "EXTRACT_TEXT"
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    scrapperServiceUrl + "/scrapper_api/scrape/scrape", requestBody, Map.class);

            if (response == null) {
                return null;
            }
            Object text = response.get("text");
            return text != null ? text.toString() : null;
        } catch (Exception e) {
            log.warn("Scraping URL '{}' selhal, config bude zaindexován bez naskrapovaného textu: {}", url, e.getMessage());
            return null;
        }
    }
}
