package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.JsonSerializerPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Infrastructure Gateway implementing JsonSerializerPort using Jackson ObjectMapper.
 */
@Component
@RequiredArgsConstructor
public class JacksonJsonSerializerGateway implements JsonSerializerPort {

    private final ObjectMapper objectMapper;

    @Override
    public String serialize(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize object to JSON", e);
        }
    }
}
