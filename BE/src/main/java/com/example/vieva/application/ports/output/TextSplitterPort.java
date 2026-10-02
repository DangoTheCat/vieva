package com.example.vieva.application.ports.output;

import java.util.List;

/**
 * Output port for splitting document text into chunks.
 * Keeps framework-specific splitters (Spring AI, LangChain, ...) out of application logic.
 */
public interface TextSplitterPort {
    List<String> split(String text);
}
