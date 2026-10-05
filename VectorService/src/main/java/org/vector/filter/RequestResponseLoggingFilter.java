package org.vector.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Loguje metodu, cestu, status a (sanitizovaná) těla každého HTTP requestu a response,
 * aby šla dohledat přesná komunikace mezi ConfigService a VectorService v produkčním logu na Renderu.
 * Vždy zapnuto, bez property přepínače.
 */
@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    static final int MAX_VALUE_LENGTH = 300;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            String requestBody = sanitizeBody(wrappedRequest.getContentAsByteArray(), wrappedRequest.getContentType());
            String responseBody = sanitizeBody(wrappedResponse.getContentAsByteArray(), wrappedResponse.getContentType());
            log.info("HTTP {} {} -> status {} | request body: {} | response body: {}",
                    wrappedRequest.getMethod(), wrappedRequest.getRequestURI(), wrappedResponse.getStatus(),
                    requestBody, responseBody);
            // KRITICKÉ: bez tohoto se tělo response nikdy nedostane ke skutečnému klientovi
            wrappedResponse.copyBodyToResponse();
        }
    }

    /**
     * Ručně dohledává JSON string literály znak po znaku - regex nad desetitisíci znaky
     * (dlouhý text k zaindexování v těle) spolehlivě spadne na StackOverflowError kvůli
     * rekurzivnímu backtrackování Java regex enginu u opakované alternace.
     */
    static String sanitizeBody(byte[] body, String contentType) {
        if (body == null || body.length == 0) {
            return "";
        }
        if (contentType != null && contentType.startsWith("multipart/")) {
            return "<multipart tělo nelogováno, " + body.length + " bajtů>";
        }

        String text = new String(body, StandardCharsets.UTF_8);
        StringBuilder result = new StringBuilder(Math.min(text.length(), 4096));
        int n = text.length();
        int i = 0;
        while (i < n) {
            char c = text.charAt(i);
            if (c != '"') {
                result.append(c);
                i++;
                continue;
            }

            int contentStart = i + 1;
            int j = contentStart;
            while (j < n && text.charAt(j) != '"') {
                j += (text.charAt(j) == '\\' && j + 1 < n) ? 2 : 1;
            }
            boolean closed = j < n;
            int contentLength = j - contentStart;

            if (closed && contentLength > MAX_VALUE_LENGTH) {
                result.append('"').append("<hodnota vynechána, ").append(contentLength).append(" znaků>").append('"');
            } else {
                result.append(text, i, closed ? j + 1 : n);
            }
            i = closed ? j + 1 : n;
        }
        return result.toString();
    }
}
