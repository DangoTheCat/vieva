package com.example.vieva.application.settings;

import lombok.Data;

/**
 * UC1.6 limits, bound from {@code vieva.import.*}.
 */
@Data
public class ImportSettings {
    private long maxFileSizeBytes = 5L * 1024 * 1024;
    private int maxRows = 2000;
}
