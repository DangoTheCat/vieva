package com.example.vieva.domain.services;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Basic near-duplicate detection for question text: Jaccard similarity of normalized word sets.
 * Cheap enough to run against the subject's bank before saving generated drafts.
 */
public final class QuestionSimilarity {

    private QuestionSimilarity() {
    }

    public static Set<String> tokens(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
        return Arrays.stream(normalized.split("[^\\p{L}\\p{N}]+"))
                .filter(token -> token.length() > 1)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        // Both sets are non-empty here (checked above), so union >= 1.
        int union = a.size() + b.size() - intersection.size();
        return (double) intersection.size() / union;
    }

    public static boolean isNearDuplicate(String candidate, Collection<Set<String>> existingTokenSets, double threshold) {
        Set<String> candidateTokens = tokens(candidate);
        for (Set<String> existing : existingTokenSets) {
            if (jaccard(candidateTokens, existing) >= threshold) {
                return true;
            }
        }
        return false;
    }
}
