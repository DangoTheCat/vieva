package com.example.vieva.application.ports.output;

import java.io.InputStream;

public interface DocumentParserPort {
    String extractText(InputStream inputStream);
}
