package org.config.service.storage.impl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class SupabaseStorageServiceImplTimeoutTest {

    @Test
    void restTemplateHasConnectAndReadTimeoutConfigured() {
        SupabaseStorageServiceImpl service = new SupabaseStorageServiceImpl(new RestTemplateBuilder());

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();

        assertThat(requestFactory).isInstanceOf(SimpleClientHttpRequestFactory.class);

        int connectTimeout = (int) ReflectionTestUtils.getField(requestFactory, "connectTimeout");
        int readTimeout = (int) ReflectionTestUtils.getField(requestFactory, "readTimeout");

        assertThat(connectTimeout).isGreaterThan(0);
        assertThat(readTimeout).isGreaterThan(0);
    }
}
