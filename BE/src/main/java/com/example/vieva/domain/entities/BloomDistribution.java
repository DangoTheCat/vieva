package com.example.vieva.domain.entities;

import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Number of questions requested per Bloom level for one generation request.
 * Invariant: counts are non-negative and add up to the requested total.
 */
public final class BloomDistribution {

    private final EnumMap<BloomLevel, Integer> counts;

    private BloomDistribution(EnumMap<BloomLevel, Integer> counts) {
        this.counts = counts;
    }

    public static BloomDistribution of(Map<BloomLevel, Integer> requested, int expectedTotal) {
        if (requested == null || requested.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_BLOOM_DISTRIBUTION, "Bloom distribution is required");
        }
        EnumMap<BloomLevel, Integer> counts = new EnumMap<>(BloomLevel.class);
        int total = 0;
        for (Map.Entry<BloomLevel, Integer> entry : requested.entrySet()) {
            if (entry.getKey() == null) {
                throw new AppException(ErrorCode.INVALID_BLOOM_DISTRIBUTION, "Bloom level must not be null");
            }
            int count = entry.getValue() == null ? 0 : entry.getValue();
            if (count < 0) {
                throw new AppException(ErrorCode.INVALID_BLOOM_DISTRIBUTION,
                        "Question count for " + entry.getKey() + " must not be negative");
            }
            if (count > 0) {
                counts.put(entry.getKey(), count);
                total += count;
            }
        }
        if (total != expectedTotal) {
            throw new AppException(ErrorCode.INVALID_BLOOM_DISTRIBUTION,
                    "Sum of Bloom distribution (" + total + ") must equal total questions (" + expectedTotal + ")");
        }
        if (total <= 0) {
            throw new AppException(ErrorCode.INVALID_BLOOM_DISTRIBUTION, "At least one question must be requested");
        }
        return new BloomDistribution(counts);
    }

    /** Rebuilds a distribution already validated earlier (e.g. loaded from storage). */
    public static BloomDistribution restore(Map<BloomLevel, Integer> stored) {
        int total = stored == null ? 0 : stored.values().stream().mapToInt(v -> v == null ? 0 : v).sum();
        return of(stored, total);
    }

    public static BloomDistribution single(BloomLevel level) {
        return of(Map.of(level, 1), 1);
    }

    public int total() {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int countFor(BloomLevel level) {
        return counts.getOrDefault(level, 0);
    }

    public Map<BloomLevel, Integer> asMap() {
        return Collections.unmodifiableMap(counts);
    }

    /**
     * Remaining demand after {@code produced} questions were accepted; levels already
     * satisfied are dropped. Returns {@code null} when nothing is missing.
     */
    public BloomDistribution remainingAfter(Map<BloomLevel, Integer> produced) {
        EnumMap<BloomLevel, Integer> remaining = new EnumMap<>(BloomLevel.class);
        counts.forEach((level, wanted) -> {
            int missing = wanted - produced.getOrDefault(level, 0);
            if (missing > 0) {
                remaining.put(level, missing);
            }
        });
        return remaining.isEmpty() ? null : new BloomDistribution(remaining);
    }
}
