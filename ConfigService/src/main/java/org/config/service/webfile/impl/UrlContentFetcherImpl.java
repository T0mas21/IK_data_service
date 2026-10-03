package org.config.service.webfile.impl;

import org.config.service.webfile.UrlContentFetcher;
import org.springframework.boot.web.client.RestTemplateBuilder;
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
    public byte[] fetch(String url) {
        return restTemplate.getForObject(URI.create(url), byte[].class);
    }
}
