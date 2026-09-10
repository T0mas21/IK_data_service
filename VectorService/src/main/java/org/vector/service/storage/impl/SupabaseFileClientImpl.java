package org.vector.service.storage.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;
import org.vector.service.storage.SupabaseFileClient;

import java.nio.charset.StandardCharsets;

@Service
public class SupabaseFileClientImpl implements SupabaseFileClient {

    private final RestTemplate restTemplate;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket}")
    private String bucket;

    public SupabaseFileClientImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public byte[] downloadFile(String storagePath) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(supabaseKey);
        headers.set("apikey", supabaseKey);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    objectUrl(storagePath), HttpMethod.GET, request, byte[].class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Stažení souboru ze Supabase Storage selhalo: " + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    private String objectUrl(String storagePath) {
        String encodedPath = UriUtils.encodePath(storagePath, StandardCharsets.UTF_8);
        return supabaseUrl + "/storage/v1/object/" + bucket + "/" + encodedPath;
    }
}
