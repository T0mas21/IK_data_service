package org.scrapper.service.impl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.scrapper.enums.ScrapperStrategy;
import org.scrapper.service.ScrapperService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ScrapperServiceImpl implements ScrapperService {

    public ScrapperServiceImpl() {
    }

    @Override
    public Object scrape(String url, int timeoutSeconds, String userAgent, ScrapperStrategy strategy) {
        Document htmlDocument = downloadRawContent(url, timeoutSeconds * 1000, userAgent);

        if (htmlDocument == null) {
            return Collections.emptyMap();
        }

        if (strategy == ScrapperStrategy.EXTRACT_TABLES) {
            return getTable(htmlDocument);
        } else if (strategy == ScrapperStrategy.EXTRACT_TEXT) {
            String textResult = extractText(htmlDocument);
            return Map.of("text", textResult);
        } else if (strategy == ScrapperStrategy.DOWNLOAD_FILE) {
            List<Map<String, String>> filesResult = extractDownloadLinks(htmlDocument);
            return Map.of("files", filesResult);
        } else if (strategy == null) {
            String textResult = extractText(htmlDocument);
            Object tablesResult = getTable(htmlDocument);
            List<Map<String, String>> filesResult = extractDownloadLinks(htmlDocument);

            return Map.of(
                    "text", textResult,
                    "tables", tablesResult,
                    "files", filesResult
            );
        } else {
            throw new IllegalArgumentException("Scrapper Error: Unsupported strategy: " + strategy);
        }
    }

    private Document downloadRawContent(String url, int timeoutMillis, String userAgent) {
        try {
            return Jsoup.connect(url)
                    .userAgent(userAgent)
                    .timeout(timeoutMillis)
                    .get();
        } catch (Exception e) {
            // TODO - add exception handler
            System.err.println("Error downloading raw content: " + e.getMessage());
            return null;
        }
    }

    private Map<String, List<Map<String, Object>>> getTable(Document htmlDocument) {
        Elements allTables = htmlDocument.select("table");
        if (allTables.isEmpty()) {
            System.out.println("Na stránce nebyla nalezena žádná tabulka.");
            return Collections.emptyMap();
        }

        Map<String, List<Map<String, Object>>> parsedDocument = new LinkedHashMap<>();

        for (int tableIndex = 0; tableIndex < allTables.size(); tableIndex++) {
            Element table = allTables.get(tableIndex);
            String tableId = "table_" + (tableIndex + 1);
            List<Map<String, Object>> tableRows = new ArrayList<>();

            Elements rows = table.select("tr");
            if (rows.isEmpty()) {
                continue;
            }

            Element headerRow = rows.get(0);
            Elements headerCells = headerRow.select("th, td");
            List<String> headers = new ArrayList<>();

            for (int i = 0; i < headerCells.size(); i++) {
                String text = headerCells.get(i).text().trim();
                headers.add(text.isEmpty() ? "column_" + (i + 1) : text);
            }

            for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
                Element row = rows.get(rowIndex);
                Elements cells = row.select("td");

                if (cells.size() != headers.size()) {
                    continue;
                }

                Map<String, Object> rowObject = new LinkedHashMap<>();

                for (int i = 0; i < headers.size(); i++) {
                    String columnName = headers.get(i);
                    Element cell = cells.get(i);
                    String cellValue = cell.text().trim();

                    Element link = cell.selectFirst("a");
                    if (link != null && link.hasAttr("href")) {
                        String rawHref = link.attr("href");

                        Map<String, String> linkData = new HashMap<>();
                        linkData.put("name", cellValue);
                        linkData.put("url", rawHref);

                        rowObject.put(columnName, List.of(linkData));
                    } else {
                        rowObject.put(columnName, cellValue);
                    }
                }

                tableRows.add(rowObject);
            }

            parsedDocument.put(tableId, tableRows);
        }

        return parsedDocument;
    }

    private String extractText(Document htmlDocument) {
        Document docCopy = htmlDocument.clone();
        docCopy.select("script, style, nav, header, footer, form, aside").remove();

        String text = docCopy.text();
        StringBuilder cleanText = new StringBuilder();
        for (String line : text.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                cleanText.append(trimmed).append("\n");
            }
        }

        return cleanText.toString().trim();
    }

    private List<Map<String, String>> extractDownloadLinks(Document htmlDocument) {
        Elements linkElements = htmlDocument.select("a[href]");
        List<Map<String, String>> downloadLinks = new ArrayList<>();

        String fileExtensionsRegex = "(?i).*\\.(pdf|zip|rar|7z|csv|xlsx?|docx?|txt|pptx?|xml|json|mp3|mp4|exe|apk)$";

        for (Element link : linkElements) {
            String href = link.attr("abs:href");
            String rawHref = link.attr("href");
            String anchorText = link.text().trim();

            boolean isDownloadAttr = link.hasAttr("download");
            boolean isFileExtension = rawHref.matches(fileExtensionsRegex);

            boolean isInsideTable = link.parents().is("table");

            boolean textMentionsFile = anchorText.matches("(?i).*(pdf|docx?|xlsx?|zip|rar|csv).*");

            if (isDownloadAttr || isFileExtension || isInsideTable || textMentionsFile) {
                Map<String, String> fileData = new LinkedHashMap<>();
                fileData.put("name", anchorText.isEmpty() ? "unnamed_file" : anchorText);
                fileData.put("url", href.isEmpty() ? rawHref : href);
                downloadLinks.add(fileData);
            }
        }

        return downloadLinks;
    }
}
