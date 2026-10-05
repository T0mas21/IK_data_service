package org.config.service.webfile;

/**
 * Výsledek stažení souboru z webové URL - {@code contentType} je typ obsahu podle
 * {@code Content-Type} hlavičky HTTP odpovědi (může být {@code null}, pokud ji server
 * neposlal).
 */
public record UrlContent(byte[] content, String contentType) {
}
