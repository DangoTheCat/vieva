package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.ParsedSection;
import com.example.vieva.application.ports.output.TextChunk;
import com.example.vieva.application.ports.output.TextSplitterPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;

import java.util.ArrayList;
import java.util.List;

/** Token splitter with page/section metadata mapped back to the application port. */
@Component
public class ChunkingEngine implements TextSplitterPort {

    private final int chunkSize;
    private final int overlap;
    private final com.example.vieva.application.ports.output.DocumentParserPort parser;
    private final Encoding encoding = Encodings.newLazyEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);

    @org.springframework.beans.factory.annotation.Autowired
    public ChunkingEngine(@Value("${vieva.rag.chunk-size-tokens:800}") int chunkSize,
                          @Value("${vieva.rag.chunk-overlap-tokens:100}") int overlap,
                          com.example.vieva.application.ports.output.DocumentParserPort parser) {
        if (chunkSize <= 0 || overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("Chunk sizes must be positive");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
        this.parser = parser;
    }

    public ChunkingEngine(int chunkSize, int overlap) {
        this(chunkSize, overlap, new TikaDocumentParserGateway());
    }

    public List<TextChunk> splitIntoChunks(byte[] fileBytes, String mimeType) {
        return split(parser.extract(fileBytes,mimeType));
    }

    public List<TextChunk> splitIntoChunks(byte[] fileBytes) {
        String mimeType = new TikaFileTypeDetectorGateway().detect(fileBytes,"document");
        return splitIntoChunks(fileBytes,mimeType);
    }

    @Override
    public List<TextChunk> split(List<ParsedSection> sections) {
        List<TextChunk> chunks = new ArrayList<>();
        for (ParsedSection section : sections) {
            if (section.text() == null || section.text().isBlank()) {
                continue;
            }
            String text = section.text();
            int start = 0;
            while (start < text.length()) {
                int end = prefixEnd(text, start, chunkSize);
                if (end == start) throw new IllegalArgumentException("chunk-size-tokens cannot fit one Unicode character");
                String chunk = text.substring(start,end);
                chunks.add(new TextChunk(chunk,encoding.countTokensOrdinary(chunk),section.pageNumber(),
                        section.pageNumber(),section.sectionTitle()));
                if (end == text.length()) break;
                int next = end;
                while (overlap > 0 && next > start) {
                    int previous = text.offsetByCodePoints(next,-1);
                    if (encoding.countTokensOrdinary(text.substring(previous,end)) > overlap) break;
                    next = previous;
                }
                start = next > start ? next : end;
            }
        }
        return chunks;
    }

    private int prefixEnd(String text, int start, int maxTokens) {
        int low = 0, high = Math.min(text.codePointCount(start,text.length()),maxTokens * 8);
        while (low < high) {
            int middle = (low + high + 1) / 2;
            int end = text.offsetByCodePoints(start,middle);
            if (encoding.countTokensOrdinary(text.substring(start,end)) <= maxTokens) low=middle;
            else high=middle-1;
        }
        return text.offsetByCodePoints(start,low);
    }
}
