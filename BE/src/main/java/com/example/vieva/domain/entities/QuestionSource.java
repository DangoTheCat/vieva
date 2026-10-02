package com.example.vieva.domain.entities;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Minh chứng xuất xứ ngữ cảnh sinh câu hỏi của AI (BR-03): đoạn tài liệu + trích dẫn.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSource {
    private UUID questionSourceId;
    private UUID questionVersionId;
    private UUID chunkId;
    private UUID documentId;
    private String documentName;
    private String citationQuote;
    private BigDecimal similarityScore;
    private Integer sourceOrder;
    private Instant createdAt;

    /**
     * True when the citation quote appears verbatim (whitespace/case-insensitive) in the chunk content.
     */
    public static boolean isGroundedIn(String citationQuote, String chunkContent) {
        if (chunkContent == null || citationQuote == null || citationQuote.isBlank()) {
            return false;
        }
        return normalize(chunkContent).contains(normalize(citationQuote));
    }

    public boolean isGroundedIn(String chunkContent) {
        return isGroundedIn(citationQuote, chunkContent);
    }

    public QuestionSource copyTo(UUID newVersionId) {
        return QuestionSource.builder()
                .questionSourceId(UUID.randomUUID())
                .questionVersionId(newVersionId)
                .chunkId(chunkId)
                .documentId(documentId)
                .documentName(documentName)
                .citationQuote(citationQuote)
                .similarityScore(similarityScore)
                .sourceOrder(sourceOrder)
                .createdAt(Instant.now())
                .build();
    }

    private static String normalize(String value) {
        return value.replaceAll("\\s+", " ").trim().toLowerCase();
    }
}
