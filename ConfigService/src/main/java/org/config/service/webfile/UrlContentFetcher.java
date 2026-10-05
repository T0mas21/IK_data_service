package org.config.service.webfile;

public interface UrlContentFetcher {

    /**
     * Stáhne bajty z libovolné webové URL, včetně typu obsahu podle {@code Content-Type}
     * hlavičky odpovědi. Nevolá se napřímo z requestu - viz
     * {@link WebFileDownloadService#downloadAndStoreAsync}.
     */
    UrlContent fetch(String url);
}
