package org.config.client;

public interface ScrapperServiceClient {

    /**
     * Vrátí naskrapovaný text ze zadané URL, nebo {@code null} pokud scraping selže
     * (chyba se pouze zaloguje, nesmí shodit operaci nad configem).
     */
    String scrapeText(String url, Integer timeoutSeconds, String userAgent);
}
