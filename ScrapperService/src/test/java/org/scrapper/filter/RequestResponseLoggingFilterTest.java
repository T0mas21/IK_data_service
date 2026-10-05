package org.scrapper.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestResponseLoggingFilterTest {

    private final RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter();

    @Test
    void shortJsonValueIsNotSanitized() {
        String body = "{\"strategy\":\"STATIC\"}";

        String sanitized = RequestResponseLoggingFilter.sanitizeBody(
                body.getBytes(StandardCharsets.UTF_8), "application/json");

        assertThat(sanitized).isEqualTo(body);
    }

    @Test
    void realisticallyLargeJsonValueDoesNotCrashSanitization() {
        // Regrese: naskrapovany text stranky muze mit stovky tisic znaku - regex nad tak
        // dlouhym retezcem spadl na StackOverflowError a filtr tim shodil celou odpoved.
        String velkyObsah = "a".repeat(328316);
        String body = "{\"strategy\":\"STATIC\",\"scrapedText\":\"" + velkyObsah + "\"}";

        String sanitized = RequestResponseLoggingFilter.sanitizeBody(
                body.getBytes(StandardCharsets.UTF_8), "application/json");

        assertThat(sanitized).contains("\"strategy\":\"STATIC\"");
        assertThat(sanitized).contains("328316");
        assertThat(sanitized).doesNotContain(velkyObsah);
    }

    @Test
    void longJsonValueIsReplacedWithPlaceholderContainingLength() {
        String longValue = "a".repeat(400);
        String body = "{\"strategy\":\"STATIC\",\"scrapedText\":\"" + longValue + "\"}";

        String sanitized = RequestResponseLoggingFilter.sanitizeBody(
                body.getBytes(StandardCharsets.UTF_8), "application/json");

        assertThat(sanitized).contains("\"strategy\":\"STATIC\"");
        assertThat(sanitized).doesNotContain(longValue);
        assertThat(sanitized).contains("400");
    }

    @Test
    void multipartBodyIsNeverLoggedVerbatim() {
        String secret = "obsah souboru, ktery se nesmi dostat do logu";
        byte[] body = secret.getBytes(StandardCharsets.UTF_8);

        String sanitized = RequestResponseLoggingFilter.sanitizeBody(
                body, "multipart/form-data; boundary=----abc123");

        assertThat(sanitized).doesNotContain(secret);
        assertThat(sanitized).contains(String.valueOf(body.length));
    }

    @Test
    void emptyBodyReturnsEmptyString() {
        assertThat(RequestResponseLoggingFilter.sanitizeBody(new byte[0], "application/json")).isEmpty();
        assertThat(RequestResponseLoggingFilter.sanitizeBody(null, "application/json")).isEmpty();
    }

    @Test
    void responseBodyReachesRealClientUnchangedAfterFiltering() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/scrapper_api/scrape/scrape");
        when(response.getStatus()).thenReturn(200);

        ByteArrayOutputStream clientOutput = new ByteArrayOutputStream();
        ServletOutputStream realOutputStream = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                // nepoužito v testu
            }

            @Override
            public void write(int b) {
                clientOutput.write(b);
            }
        };
        when(response.getOutputStream()).thenReturn(realOutputStream);

        byte[] responseBodyFromChain = "{\"text\":\"ok\"}".getBytes(StandardCharsets.UTF_8);

        FilterChain wrappedChainCapture = (req, res) -> {
            ContentCachingResponseWrapper wrappedResponse = (ContentCachingResponseWrapper) res;
            wrappedResponse.getOutputStream().write(responseBodyFromChain);
        };

        filter.doFilterInternal(request, response, wrappedChainCapture);

        assertThat(clientOutput.toByteArray()).isEqualTo(responseBodyFromChain);
    }
}
