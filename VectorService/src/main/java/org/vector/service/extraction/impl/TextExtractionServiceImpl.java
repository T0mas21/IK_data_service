package org.vector.service.extraction.impl;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.vector.service.extraction.TextExtractionService;

import java.io.ByteArrayInputStream;

@Service
public class TextExtractionServiceImpl implements TextExtractionService {

    private final ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();

    @Override
    public String extractText(byte[] content) {
        try {
            Document document = parser.parse(new ByteArrayInputStream(content));
            return document.text();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Text ze souboru se nepodařilo extrahovat: " + e.getMessage(),
                    e
            );
        }
    }
}
