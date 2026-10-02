package com.example.vieva.application.ports.output;

/**
 * Raised by AI adapters (LLM / embedding) when the provider fails. The message must not contain
 * secrets or the full prompt.
 */
public class AiServiceException extends RuntimeException {
    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
