package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.ParsedSection;
import com.example.vieva.application.ports.output.TextChunk;
import com.example.vieva.application.ports.output.TextSplitterPort;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Spring AI's token splitter with page/section metadata mapped back to the application port. */
@Component
public class TokenTextSplitterGateway implements TextSplitterPort {

    private final TokenTextSplitter splitter;
    private final Encoding encoding = Encodings.newLazyEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);

    public TokenTextSplitterGateway(@Value("${vieva.rag.chunk-size-tokens:800}") int chunkSize,
                                    @Value("${vieva.rag.min-chunk-tokens:20}") int minChunkTokens) {
        if (chunkSize <= 0 || minChunkTokens < 0) {
            throw new IllegalArgumentException("Chunk sizes must be positive");
        }
        this.splitter = TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withMinChunkSizeChars(Math.max(1, minChunkTokens * 4))
                .withMinChunkLengthToEmbed(0)
                .withMaxNumChunks(Integer.MAX_VALUE)
                .withKeepSeparator(true)
                .build();
    }

    @Override
    public List<TextChunk> split(List<ParsedSection> sections) {
        List<TextChunk> chunks = new ArrayList<>();
        for (ParsedSection section : sections) {
            if (section.text() == null || section.text().isBlank()) {
                continue;
            }
            Map<String, Object> metadata = new HashMap<>();
            if (section.pageNumber() != null) metadata.put("page", section.pageNumber());
            if (section.sectionTitle() != null) metadata.put("section", section.sectionTitle());
            for (Document document : splitter.split(new Document(section.text(), metadata))) {
                String text = document.getText();
                if (text == null || text.isBlank()) {
                    continue;
                }
                chunks.add(new TextChunk(text, tokenCount(document), section.pageNumber(),
                        section.pageNumber(), section.sectionTitle()));
            }
        }
        return chunks;
    }

    private int tokenCount(Document document) {
        return encoding.encode(document.getText()).size();
    }
}
