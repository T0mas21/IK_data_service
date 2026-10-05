package org.config.service.webfile.impl;

import org.config.service.webfile.UrlContent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UrlContentFetcherImplTest {

    @Test
    void restTemplateHasConnectAndReadTimeoutConfigured() {
        UrlContentFetcherImpl fetcher = new UrlContentFetcherImpl(new RestTemplateBuilder());

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(fetcher, "restTemplate");
        ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();

        assertThat(requestFactory).isInstanceOf(SimpleClientHttpRequestFactory.class);

        int connectTimeout = (int) ReflectionTestUtils.getField(requestFactory, "connectTimeout");
        int readTimeout = (int) ReflectionTestUtils.getField(requestFactory, "readTimeout");

        assertThat(connectTimeout).isGreaterThan(0);
        assertThat(readTimeout).isGreaterThan(0);
    }

    @Test
    void fetchVraciObsahIContentTypeZHttpOdpovedi() {
        UrlContentFetcherImpl fetcher = new UrlContentFetcherImpl(new RestTemplateBuilder());
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(fetcher, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        byte[] obsah = "%PDF-1.4 obsah".getBytes();
        server.expect(requestTo("https://example.com/priloha.pdf"))
                .andRespond(withSuccess(obsah, MediaType.APPLICATION_PDF));

        UrlContent result = fetcher.fetch("https://example.com/priloha.pdf");

        assertThat(result.content()).isEqualTo(obsah);
        assertThat(result.contentType()).isEqualTo("application/pdf");
        server.verify();
    }
}
