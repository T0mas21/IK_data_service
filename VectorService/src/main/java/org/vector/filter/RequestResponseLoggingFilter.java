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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loguje metodu, cestu, status a (sanitizovaná) těla každého HTTP requestu a response,
 * aby šla dohledat přesná komunikace mezi ConfigService a VectorService v produkčním logu na Renderu.
 * Vždy zapnuto, bez property přepínače.
 */
@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    static final int MAX_VALUE_LENGTH = 300;

    // JSON string literál - uvozovky, uvnitř cokoliv kromě neescapované uvozovky/zpětného lomítka, nebo escapovaná dvojice
    private static final Pattern JSON_STRING_VALUE = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"");

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

    static String sanitizeBody(byte[] body, String contentType) {
        if (body == null || body.length == 0) {
            return "";
        }
        if (contentType != null && contentType.startsWith("multipart/")) {
            return "<multipart tělo nelogováno, " + body.length + " bajtů>";
        }

        String content = new String(body, StandardCharsets.UTF_8);
        Matcher matcher = JSON_STRING_VALUE.matcher(content);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group();
            int originalLength = match.length() - 2; // bez okrajových uvozovek
            if (originalLength > MAX_VALUE_LENGTH) {
                matcher.appendReplacement(result,
                        Matcher.quoteReplacement("\"<hodnota vynechána, " + originalLength + " znaků>\""));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
