package org.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestResponseLoggingFilterTest {

    @Test
    void sanitizeBodyPonechaKratkouJsonHodnotuBezeZmeny() {
        String json = "{\"fileName\":\"a.pdf\"}";

        String result = RequestResponseLoggingFilter.sanitizeBody(json.getBytes(StandardCharsets.UTF_8), "application/json");

        assertThat(result).isEqualTo(json);
    }

    @Test
    void sanitizeBodyNahradiDlouhouHodnotuPlaceholderemAZachovaZbytekJsonu() {
        String dlouhyObsah = "a".repeat(400);
        String json = "{\"fileName\":\"soubor.pdf\",\"content\":\"" + dlouhyObsah + "\"}";

        String result = RequestResponseLoggingFilter.sanitizeBody(json.getBytes(StandardCharsets.UTF_8), "application/json");

        assertThat(result).contains("\"fileName\":\"soubor.pdf\"");
        assertThat(result).contains("400");
        assertThat(result).doesNotContain(dlouhyObsah);
    }

    @Test
    void sanitizeBodyNikdyNezalogujeSyroveMultipartTelo() {
        String binarniObsah = "%PDF-1.4 binarni obsah souboru, ktery se nesmi dostat do logu";
        String telo = "--boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a.pdf\"\r\n\r\n"
                + binarniObsah + "\r\n--boundary--";

        String result = RequestResponseLoggingFilter.sanitizeBody(
                telo.getBytes(StandardCharsets.UTF_8), "multipart/form-data; boundary=boundary");

        assertThat(result).doesNotContain(binarniObsah);
        assertThat(result).doesNotContain("%PDF");
    }

    @Test
    void sanitizeBodyVratiPrazdnyRetezecProPrazdneTelo() {
        assertThat(RequestResponseLoggingFilter.sanitizeBody(new byte[0], "application/json")).isEmpty();
        assertThat(RequestResponseLoggingFilter.sanitizeBody(null, "application/json")).isEmpty();
    }

    @Test
    void doFilterInternalPredaTeloResponseBezeZmenyKeSkutecnemuKlientovi() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/configs");
        when(request.getInputStream()).thenReturn(emptyServletInputStream());
        when(response.getStatus()).thenReturn(200);

        ByteArrayOutputStream skutecnyKlientskyStream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(toServletOutputStream(skutecnyKlientskyStream));

        byte[] ocekavaneTelo = "{\"result\":\"ok\"}".getBytes(StandardCharsets.UTF_8);

        // Chain zapise telo do wrapnute response - presne to, co by normalne zapsal controller.
        org.mockito.Mockito.doAnswer(invocation -> {
            ContentCachingResponseWrapper wrappedResponse = invocation.getArgument(1);
            wrappedResponse.setContentType("application/json");
            wrappedResponse.getOutputStream().write(ocekavaneTelo);
            return null;
        }).when(filterChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter();
        filter.doFilterInternal(request, response, filterChain);

        assertThat(skutecnyKlientskyStream.toByteArray()).isEqualTo(ocekavaneTelo);
    }

    private static jakarta.servlet.ServletInputStream emptyServletInputStream() {
        ByteArrayInputStream delegate = new ByteArrayInputStream(new byte[0]);
        return new jakarta.servlet.ServletInputStream() {
            @Override
            public boolean isFinished() {
                return delegate.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(jakarta.servlet.ReadListener readListener) {
                // v testu neni potreba asynchronni cteni
            }

            @Override
            public int read() {
                return delegate.read();
            }
        };
    }

    private static ServletOutputStream toServletOutputStream(ByteArrayOutputStream cil) {
        return new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                // v testu neni potreba asynchronni zapis
            }

            @Override
            public void write(int b) {
                cil.write(b);
            }
        };
    }
}
