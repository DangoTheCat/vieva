package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.ParsedSection;
import com.example.vieva.application.ports.output.TextChunk;
import com.example.vieva.application.ports.output.TextSplitterPort;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Sentence-aware chunking measured in cl100k tokens (the tokenizer of OpenAI embedding models).
 * Chunks hold at most {@code chunk-size} tokens and repeat about {@code chunk-overlap} tokens of
 * the previous chunk so an answer cut at a boundary stays retrievable. Page range and section
 * title of each chunk are kept as metadata.
 */
@Component
public class TokenTextSplitterGateway implements TextSplitterPort {

    private final Encoding encoding = Encodings.newLazyEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);
    private final int chunkSize;
    private final int overlap;
    private final int minChunkTokens;

    public TokenTextSplitterGateway(@Value("${vieva.rag.chunk-size-tokens:800}") int chunkSize,
                                    @Value("${vieva.rag.chunk-overlap-tokens:100}") int overlap,
                                    @Value("${vieva.rag.min-chunk-tokens:20}") int minChunkTokens) {
        if (chunkSize <= 0 || overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("Require 0 <= chunk-overlap-tokens < chunk-size-tokens");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
        this.minChunkTokens = minChunkTokens;
    }

    private record Unit(String text, int tokens, Integer page, String section) {
    }

    @Override
    public List<TextChunk> split(List<ParsedSection> sections) {
        List<Unit> units = new ArrayList<>();
        for (ParsedSection section : sections) {
            if (section.text() == null || section.text().isBlank()) {
                continue;
            }
            for (String sentence : sentences(section.text())) {
                addUnit(units, sentence, section.pageNumber(), section.sectionTitle());
            }
        }

        List<TextChunk> chunks = new ArrayList<>();
        Deque<Unit> window = new ArrayDeque<>();
        List<Unit> fresh = new ArrayList<>();
        int windowTokens = 0;
        for (Unit unit : units) {
            if (windowTokens + unit.tokens() > chunkSize && !fresh.isEmpty()) {
                chunks.add(toChunk(window));
                // Carry the tail of the chunk over as overlap.
                Deque<Unit> carry = new ArrayDeque<>();
                int carried = 0;
                while (!window.isEmpty() && carried + window.peekLast().tokens() <= overlap) {
                    Unit last = window.pollLast();
                    carry.addFirst(last);
                    carried += last.tokens();
                }
                window = carry;
                windowTokens = carried;
                fresh.clear();
            }
            window.addLast(unit);
            windowTokens += unit.tokens();
            fresh.add(unit);
        }
        if (!fresh.isEmpty()) {
            int freshTokens = fresh.stream().mapToInt(Unit::tokens).sum();
            if (freshTokens < minChunkTokens && !chunks.isEmpty()
                    && chunks.get(chunks.size() - 1).tokenCount() + freshTokens <= chunkSize + minChunkTokens) {
                // Append a tiny remainder to the previous chunk instead of indexing a near-empty chunk.
                TextChunk previous = chunks.remove(chunks.size() - 1);
                TextChunk remainder = toChunk(new ArrayDeque<>(fresh));
                chunks.add(new TextChunk(previous.text() + " " + remainder.text(),
                        previous.tokenCount() + remainder.tokenCount(),
                        previous.pageStart() != null ? previous.pageStart() : remainder.pageStart(),
                        remainder.pageEnd() != null ? remainder.pageEnd() : previous.pageEnd(),
                        previous.sectionTitle()));
            } else {
                chunks.add(toChunk(window));
            }
        }
        return chunks;
    }

    private void addUnit(List<Unit> units, String sentence, Integer page, String section) {
        String text = sentence.trim();
        if (text.isEmpty()) {
            return;
        }
        IntArrayList tokens = encoding.encode(text);
        if (tokens.size() <= chunkSize) {
            units.add(new Unit(text, tokens.size(), page, section));
            return;
        }
        // A single sentence longer than a chunk: hard-split on token windows.
        int step = chunkSize - overlap;
        for (int start = 0; start < tokens.size(); start += step) {
            int end = Math.min(start + chunkSize, tokens.size());
            IntArrayList slice = new IntArrayList(end - start);
            for (int i = start; i < end; i++) {
                slice.add(tokens.get(i));
            }
            units.add(new Unit(encoding.decode(slice), end - start, page, section));
            if (end == tokens.size()) {
                break;
            }
        }
    }

    private TextChunk toChunk(Deque<Unit> window) {
        StringBuilder text = new StringBuilder();
        int tokens = 0;
        Integer pageStart = null;
        Integer pageEnd = null;
        String section = null;
        for (Unit unit : window) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(unit.text());
            tokens += unit.tokens();
            if (unit.page() != null) {
                pageStart = pageStart == null ? unit.page() : Math.min(pageStart, unit.page());
                pageEnd = pageEnd == null ? unit.page() : Math.max(pageEnd, unit.page());
            }
            if (section == null) {
                section = unit.section();
            }
        }
        return new TextChunk(text.toString(), tokens, pageStart, pageEnd, section);
    }

    /** Paragraphs first, then sentence boundaries (., !, ?, …) followed by whitespace. */
    static List<String> sentences(String text) {
        List<String> result = new ArrayList<>();
        for (String paragraph : text.split("\\n+")) {
            for (String sentence : paragraph.split("(?<=[.!?…])\\s+")) {
                if (!sentence.isBlank()) {
                    result.add(sentence);
                }
            }
        }
        return result;
    }
}
