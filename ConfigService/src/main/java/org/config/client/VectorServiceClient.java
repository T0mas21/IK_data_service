package org.config.client;

import org.config.data.model.File;

import java.util.List;

public interface VectorServiceClient {

    /**
     * Zaindexuje (přepíše) veškerý obsah daného configu ve Vector službě.
     * Chyba se pouze zaloguje, nesmí shodit operaci nad configem (best-effort).
     */
    void indexConfig(Long configId, String customText, String scrapedText, List<File> files);

    /**
     * Smaže veškeré zaindexované chunky daného configu. Best-effort, chyba se pouze zaloguje.
     */
    void deleteConfig(Long configId);
}
