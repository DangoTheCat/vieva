package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.DocumentParserPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TikaDocumentParserGateway implements DocumentParserPort {

    @Override
    public String extractText(InputStream inputStream) {
        try {
            TikaDocumentReader reader = new TikaDocumentReader(new InputStreamResource(inputStream));
            List<Document> documents = reader.get();
            if (documents == null || documents.isEmpty()) {
                return "";
            }
            return documents.stream()
                    .map(Document::getText)
                    .filter(t -> t != null && !t.isBlank())
                    .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            log.error("Failed to parse document using Tika", e);
            throw new RuntimeException("Document text extraction failed: " + e.getMessage(), e);
        }
    }
}
