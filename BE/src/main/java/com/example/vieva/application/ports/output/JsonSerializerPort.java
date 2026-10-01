package com.example.vieva.application.ports.output;

/**
 * Output Port for serializing domain/audit data structures to JSON strings.
 * Keeps application use cases completely decoupled from third-party serialization libraries (e.g. Jackson, Gson).
 */
public interface JsonSerializerPort {
    String serialize(Object object);
}
