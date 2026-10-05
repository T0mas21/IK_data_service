package org.vector.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestResponseLoggingFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private final RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter();

    @Test
    void sanitizeBody_KratkaHodnota_ZustaneBezeZmeny() {
        String json = "{\"configId\":42}";

        String result = RequestResponseLoggingFilter.sanitizeBody(json.getBytes(StandardCharsets.UTF_8), "application/json");

        assertEquals(json, result);
    }

    @Test
    void sanitizeBody_DlouhaHodnota_NahrazenaZastupnymTextem() {
        String dlouhyText = "a".repeat(400);
        String json = "{\"text\":\"" + dlouhyText + "\"}";

        String result = RequestResponseLoggingFilter.sanitizeBody(json.getBytes(StandardCharsets.UTF_8), "application/json");

        assertFalse(result.contains(dlouhyText), "Původní dlouhý obsah nesmí být v logu");
        assertTrue(result.contains("400"), "Placeholder musí obsahovat počet znaků vynechané hodnoty");
        assertTrue(result.contains("\"text\":"), "Zbytek JSONu musí zůstat čitelný");
    }

    @Test
    void sanitizeBody_MultipartTelo_NikdyNezalogovanoDoslovne() {
        byte[] body = "--boundary\r\nContent-Disposition: form-data; name=\"file\"\r\n\r\ntajny obsah\r\n--boundary--"
                .getBytes(StandardCharsets.UTF_8);

        String result = RequestResponseLoggingFilter.sanitizeBody(body, "multipart/form-data; boundary=boundary");

        assertFalse(result.contains("tajny obsah"));
        assertTrue(result.contains(String.valueOf(body.length)));
    }

    @Test
    void sanitizeBody_PrazdneTelo_VracíPrazdnyRetezec() {
        String result = RequestResponseLoggingFilter.sanitizeBody(new byte[0], "application/json");

        assertEquals("", result);
    }

    @Test
    void doFilterInternal_TeloResponse_ProjdeKeSkutecnemuKlientoviBezeZmeny() throws Exception {
        ByteArrayOutputStream skutecnyKlient = new ByteArrayOutputStream();
        ServletOutputStream surovyVystup = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                // v testu nepotřeba
            }

            @Override
            public void write(int b) throws IOException {
                skutecnyKlient.write(b);
            }
        };

        byte[] ocekavanaOdpoved = "{\"result\":\"ok\"}".getBytes(StandardCharsets.UTF_8);

        lenient().when(request.getMethod()).thenReturn("POST");
        lenient().when(request.getRequestURI()).thenReturn("/scrapper_api/vector/index");
        lenient().when(request.getContentType()).thenReturn("application/json");
        when(response.getOutputStream()).thenReturn(surovyVystup);
        lenient().when(response.isCommitted()).thenReturn(false);
        lenient().when(response.getStatus()).thenReturn(200);

        doAnswer(invocation -> {
            HttpServletResponse wrappedResponseArg = invocation.getArgument(1);
            wrappedResponseArg.getOutputStream().write(ocekavanaOdpoved);
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("{\"result\":\"ok\"}", skutecnyKlient.toString(StandardCharsets.UTF_8));
    }
}
