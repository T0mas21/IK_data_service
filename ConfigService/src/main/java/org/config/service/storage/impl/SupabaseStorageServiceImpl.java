package org.config.service.storage.impl;

import org.config.service.storage.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Service
public class SupabaseStorageServiceImpl implements SupabaseStorageService {

    private final RestTemplate restTemplate;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket}")
    private String bucket;

    public SupabaseStorageServiceImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public String uploadFile(String storagePath, byte[] content, String contentType) {
        HttpHeaders headers = buildAuthHeaders();
        headers.setContentType(contentType != null && !contentType.isBlank()
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM);

        HttpEntity<byte[]> request = new HttpEntity<>(content, headers);

        try {
            restTemplate.exchange(objectUrl(storagePath), HttpMethod.POST, request, String.class);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Nahrání souboru do Supabase Storage selhalo: " + e.getResponseBodyAsString(),
                    e
            );
        }

        return storagePath;
    }

    @Override
    public void deleteFile(String storagePath) {
        HttpEntity<Void> request = new HttpEntity<>(buildAuthHeaders());

        try {
            restTemplate.exchange(objectUrl(storagePath), HttpMethod.DELETE, request, String.class);
        } catch (HttpClientErrorException.NotFound ignored) {
            // soubor v Supabase Storage už neexistuje
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Smazání souboru ze Supabase Storage selhalo: " + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    private String objectUrl(String storagePath) {
        String encodedPath = UriUtils.encodePath(storagePath, StandardCharsets.UTF_8);
        return supabaseUrl + "/storage/v1/object/" + bucket + "/" + encodedPath;
    }

    private HttpHeaders buildAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(supabaseKey);
        headers.set("apikey", supabaseKey);
        return headers;
    }
}