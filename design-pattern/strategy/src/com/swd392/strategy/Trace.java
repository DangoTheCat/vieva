package com.swd392.strategy;

import java.util.Locale;

/** Console helper that prints the demo trace. It is not a participant of the Strategy pattern. */
final class Trace {

    private Trace() {
    }

    static void log(String role, String diagramMarker, String message) {
        System.out.printf("  %-18s %-6s %s%n", "[" + role + "]", diagramMarker, message);
    }

    static String vnd(long amount) {
        return String.format(Locale.US, "%,d VND", amount);
    }
}
