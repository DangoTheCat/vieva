package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.ParsedSection;
import com.example.vieva.application.ports.output.TextChunk;
import com.example.vieva.application.ports.output.TextSplitterPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;

import java.util.ArrayList;
import java.util.List;

/** Token splitter with page/section metadata mapped back to the application port. */
@Component
public class TokenTextSplitterGateway implements TextSplitterPort {

    private final int chunkSize;
    private final int minChunkTokens;
    private final Encoding encoding = Encodings.newLazyEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);

    public TokenTextSplitterGateway(@Value("${vieva.rag.chunk-size-tokens:800}") int chunkSize,
                                    @Value("${vieva.rag.min-chunk-tokens:20}") int minChunkTokens) {
        if (chunkSize <= 0 || minChunkTokens < 0) {
            throw new IllegalArgumentException("Chunk sizes must be positive");
        }
        this.chunkSize = chunkSize;
        this.minChunkTokens = minChunkTokens;
    }

    @Override
    public List<TextChunk> split(List<ParsedSection> sections) {
        List<TextChunk> chunks = new ArrayList<>();
        for (ParsedSection section : sections) {
            if (section.text() == null || section.text().isBlank()) {
                continue;
            }
            IntArrayList tokens = encoding.encode(section.text());
            for (int start = 0; start < tokens.size(); start += chunkSize) {
                int end = Math.min(tokens.size(), start + chunkSize);
                IntArrayList window = new IntArrayList(end - start);
                for (int i = start; i < end; i++) {
                    window.add(tokens.get(i));
                }
                if (window.size() >= minChunkTokens) {
                    chunks.add(new TextChunk(encoding.decode(window), window.size(), section.pageNumber(),
                            section.pageNumber(), section.sectionTitle()));
                }
            }
        }
        return chunks;
    }
}
