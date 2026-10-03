package com.example.vieva.application.ports.output;

/**
 * @param storageKey opaque key understood by the storage adapter
 * @param url        location shown to clients (may equal the key for private storage)
 */
public record StoredFile(String storageKey, String url) {
}
