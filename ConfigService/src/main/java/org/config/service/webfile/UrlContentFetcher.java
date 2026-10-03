package org.config.service.webfile;

public interface UrlContentFetcher {

    /**
     * Stáhne bajty z libovolné webové URL. Nevolá se napřímo z requestu - viz
     * {@link WebFileDownloadService#downloadAndStoreAsync}.
     */
    byte[] fetch(String url);
}
