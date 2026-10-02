package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.TextSplitterPort;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SpringAiTextSplitterGateway implements TextSplitterPort {

    @Override
    public List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> splitDocuments = splitter.apply(List.of(new Document(text)));
        if (splitDocuments == null || splitDocuments.isEmpty()) {
            return List.of(text);
        }
        List<String> chunkTexts = splitDocuments.stream()
                .map(Document::getText)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());
        return chunkTexts.isEmpty() ? List.of(text) : chunkTexts;
    }
}
