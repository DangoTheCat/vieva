package com.example.vieva.application.usecases.user;

/**
 * Published inside the admin create-user transaction; the welcome email is sent after commit,
 * so a rolled-back account never gets an email.
 * Holds the plaintext temporary password in memory only — it is never persisted or logged.
 */
public record AccountCreatedEvent(String email, String fullName, String roleCode, String temporaryPassword) {

    @Override
    public String toString() {
        return "AccountCreatedEvent[email=" + email + ", roleCode=" + roleCode + "]";
    }
}
