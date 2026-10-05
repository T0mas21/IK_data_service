package org.config.service.webfile.impl;

import org.config.service.webfile.UrlContent;
import org.config.service.webfile.UrlContentFetcher;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;

@Component
public class UrlContentFetcherImpl implements UrlContentFetcher {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private final RestTemplate restTemplate;

    public UrlContentFetcherImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setReadTimeout(READ_TIMEOUT)
                .build();
    }

    @Override
    public UrlContent fetch(String url) {
        ResponseEntity<byte[]> response = restTemplate.exchange(URI.create(url), HttpMethod.GET, null, byte[].class);
        MediaType contentType = response.getHeaders().getContentType();
        return new UrlContent(response.getBody(), contentType != null ? contentType.toString() : null);
    }
}
