package org.config.client.impl;

import org.config.client.VectorServiceClient;
import org.config.data.model.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class VectorServiceClientImpl implements VectorServiceClient {

    private static final Logger log = LoggerFactory.getLogger(VectorServiceClientImpl.class);

    private final RestTemplate restTemplate;

    @Value("${vector-service.url}")
    private String vectorServiceUrl;

    public VectorServiceClientImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public void indexConfig(Long configId, String customText, String scrapedText, List<File> files) {
        List<Map<String, Object>> fileRefs = files == null ? List.of() : files.stream()
                .map(f -> Map.<String, Object>of(
                        "fileId", f.getId(),
                        "fileName", f.getFileName(),
                        "storagePath", f.getStoragePath(),
                        "fileType", f.getFileType() != null ? f.getFileType() : ""
                ))
                .toList();

        Map<String, Object> requestBody = Map.of(
                "configId", configId,
                "customText", customText != null ? customText : "",
                "scrapedText", scrapedText != null ? scrapedText : "",
                "files", fileRefs
        );

        try {
            restTemplate.postForObject(vectorServiceUrl + "/scrapper_api/vector/index", requestBody, Map.class);
        } catch (Exception e) {
            log.warn("Zaindexování configu {} ve Vector službě selhalo: {}", configId, e.getMessage());
        }
    }

    @Override
    public void deleteConfig(Long configId) {
        try {
            restTemplate.delete(vectorServiceUrl + "/scrapper_api/vector/configs/{configId}", configId);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                log.warn("Smazání indexu configu {} ve Vector službě selhalo: {}", configId, e.getMessage());
            }
        } catch (Exception e) {
            log.warn("Smazání indexu configu {} ve Vector službě selhalo: {}", configId, e.getMessage());
        }
    }
}
