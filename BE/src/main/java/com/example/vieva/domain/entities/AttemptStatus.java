package com.example.vieva.domain.entities;

public enum AttemptStatus {
    READY,
    IN_PROGRESS,
    RECONNECTING,
    FINALIZING,
    COMPLETED,
    INTERRUPTED,
    TIMED_OUT,
    CANCELLED
}
