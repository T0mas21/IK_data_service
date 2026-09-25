package org.scrapper.service.impl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScrapperServiceImplExtractDownloadLinksTest {

    private final ScrapperServiceImpl scrapperService = new ScrapperServiceImpl();

    @Test
    void plainWebsiteLinkInsideTableIsNotConsideredFile() {
        Document doc = Jsoup.parse("""
                <table><tr>
                    <td>Město Bor</td>
                    <td><a href="http://www.relsie.cz/"><u>Relsie spol. s r. o.</u></a></td>
                </tr></table>
                """);

        List<Map<String, String>> files = scrapperService.extractDownloadLinks(doc);

        assertThat(files).extracting(f -> f.get("url")).doesNotContain("http://www.relsie.cz/");
    }

    @Test
    void pdfLinkWithQueryStringInsideTableIsDetectedByExtension() {
        Document doc = Jsoup.parse("""
                <table><tr>
                    <td>Město Bor</td>
                    <td><a data-provider="media" target="_self"
                           href="https://www.dia.gov.cz/media/2959/download/atestace_Bor.pdf?v=1">
                        <u>DR 1020</u></a> (pdf, 49 kB)</td>
                </tr></table>
                """);

        List<Map<String, String>> files = scrapperService.extractDownloadLinks(doc);

        assertThat(files).extracting(f -> f.get("url"))
                .contains("https://www.dia.gov.cz/media/2959/download/atestace_Bor.pdf?v=1");
    }

    @Test
    void pdfExtensionWithoutQueryStringIsDetected() {
        Document doc = Jsoup.parse("""
                <a href="https://example.com/report.pdf">Report</a>
                """);

        List<Map<String, String>> files = scrapperService.extractDownloadLinks(doc);

        assertThat(files).extracting(f -> f.get("url")).contains("https://example.com/report.pdf");
    }

    @Test
    void txtExtensionIsDetected() {
        Document doc = Jsoup.parse("""
                <a href="https://example.com/notes.txt">Notes</a>
                """);

        List<Map<String, String>> files = scrapperService.extractDownloadLinks(doc);

        assertThat(files).extracting(f -> f.get("url")).contains("https://example.com/notes.txt");
    }

    @Test
    void downloadAttributeAloneIsNotEnoughWithoutPdfOrTxtExtension() {
        Document doc = Jsoup.parse("""
                <a href="https://example.com/data" download>Stahnout data</a>
                """);

        List<Map<String, String>> files = scrapperService.extractDownloadLinks(doc);

        assertThat(files).isEmpty();
    }

    @Test
    void anchorTextMentioningFileTypeAloneIsNotEnoughWithoutExtension() {
        Document doc = Jsoup.parse("""
                <a href="https://example.com/getFile">Stahnout PDF verzi</a>
                """);

        List<Map<String, String>> files = scrapperService.extractDownloadLinks(doc);

        assertThat(files).isEmpty();
    }

    @Test
    void imageLinkIsNotConsideredFile() {
        Document doc = Jsoup.parse("""
                <a href="https://example.com/photo.jpg" download>Fotka</a>
                """);

        List<Map<String, String>> files = scrapperService.extractDownloadLinks(doc);

        assertThat(files).isEmpty();
    }

    @Test
    void videoLinkIsNotConsideredFile() {
        Document doc = Jsoup.parse("""
                <a href="https://example.com/video.mp4">Video</a>
                """);

        List<Map<String, String>> files = scrapperService.extractDownloadLinks(doc);

        assertThat(files).isEmpty();
    }

    @Test
    void archiveAndOfficeExtensionsAreNoLongerConsideredFiles() {
        Document doc = Jsoup.parse("""
                <a href="https://example.com/archive.zip">Zip</a>
                <a href="https://example.com/doc.docx">Doc</a>
                <a href="https://example.com/sheet.xlsx">Sheet</a>
                <a href="https://example.com/data.csv">Csv</a>
                """);

        List<Map<String, String>> files = scrapperService.extractDownloadLinks(doc);

        assertThat(files).isEmpty();
    }

    @Test
    void plainWebsiteLinkOutsideTableIsNotConsideredFile() {
        Document doc = Jsoup.parse("""
                <a href="https://www.relsie.cz/">Relsie spol. s r. o.</a>
                """);

        List<Map<String, String>> files = scrapperService.extractDownloadLinks(doc);

        assertThat(files).isEmpty();
    }
}
