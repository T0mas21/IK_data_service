package org.config.filter;

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
 * Loguje přesné tělo každého HTTP requestu a response, aby šlo dohledat komunikaci
 * s externí integrační platformou přímo v produkčním logu na Renderu.
 */
@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    static final int MAX_VALUE_LENGTH = 300;

    private static final Pattern JSON_STRING_LITERAL = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"");

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

            // Nejrizikovejsi krok - bez nej by se telo response nikdy nedostalo ke skutecnemu klientovi.
            wrappedResponse.copyBodyToResponse();
        }
    }

    /**
     * Sanitizuje tělo HTTP requestu/response pro potřeby logování - nahradí dlouhé
     * JSON hodnoty (typicky base64 obsah souboru) zástupným textem, ať log nezahltí.
     */
    static String sanitizeBody(byte[] body, String contentType) {
        if (body == null || body.length == 0) {
            return "";
        }
        if (contentType != null && contentType.startsWith("multipart/")) {
            return "<multipart tělo nelogováno, " + body.length + " bajtů>";
        }

        String text = new String(body, StandardCharsets.UTF_8);
        Matcher matcher = JSON_STRING_LITERAL.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group();
            int contentLength = match.length() - 2; // bez uvozovek
            if (contentLength > MAX_VALUE_LENGTH) {
                matcher.appendReplacement(result,
                        Matcher.quoteReplacement("\"<hodnota vynechána, " + contentLength + " znaků>\""));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(match));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
