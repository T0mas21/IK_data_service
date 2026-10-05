package org.scrapper.filter;

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
 * Loguje přesné tělo každého HTTP requestu a response (metoda, cesta, status, body),
 * aby šlo z produkčního logu na Renderu dohledat skutečnou komunikaci mezi službami.
 * Vždy zapnuté, bez přepínání přes property.
 */
@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    private static final int MAX_VALUE_LENGTH = 300;

    // JSON string literál (klíč i hodnota) - uvozovky, escapované znaky uvnitř.
    private static final Pattern JSON_STRING_VALUE = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            String requestBody = sanitizeBody(wrappedRequest.getContentAsByteArray(), wrappedRequest.getContentType());
            String responseBody = sanitizeBody(wrappedResponse.getContentAsByteArray(), wrappedResponse.getContentType());

            log.info("HTTP {} {} -> status {}, request body: {}, response body: {}",
                    request.getMethod(), request.getRequestURI(), wrappedResponse.getStatus(),
                    requestBody, responseBody);

            // KRITICKÉ: bez tohoto se tělo response nikdy nedostane ke skutečnému klientovi.
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
            int innerLength = match.length() - 2; // bez uvozovek
            if (innerLength > MAX_VALUE_LENGTH) {
                matcher.appendReplacement(result,
                        Matcher.quoteReplacement("\"<hodnota vynechána, " + innerLength + " znaků>\""));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(match));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
