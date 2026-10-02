package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.FileTypeDetectorPort;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

/**
 * Magic-byte MIME detection (Tika). The file name is only a hint for container formats.
 */
@Component
public class TikaFileTypeDetectorGateway implements FileTypeDetectorPort {

    private final Tika tika = new Tika();

    @Override
    public String detect(byte[] content, String filename) {
        return tika.detect(content, filename);
    }
}
