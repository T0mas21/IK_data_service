package org.scrapper.service;

import org.scrapper.enums.ScrapperStrategy;

public interface ScrapperService {

    /**
     * Stáhne a zpracuje HTML ze zadané URL dle zvolené strategie.
     *
     * @param url cílová webová stránka
     * @param timeoutSeconds maximální doba čekání v sekundách
     * @param userAgent User-Agent hlavička pro požadavek
     * @param strategy požadovaný způsob zpracování
     * @return Zpracovaná data
     */
    Object scrape(String url, int timeoutSeconds, String userAgent, ScrapperStrategy strategy);
}