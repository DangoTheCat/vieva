package com.example.vieva.application.settings;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Self-registration policy, bound from {@code vieva.auth.*} (plan item 3.4).
 * Only addresses of these domains (or their sub-domains) may register themselves as students;
 * every other account is created by an administrator. An empty list leaves registration open.
 */
@Data
public class AuthSettings {
    private List<String> allowedRegistrationDomains = new ArrayList<>(List.of("fpt.edu.vn"));

    public boolean isRegistrationAllowed(String email) {
        if (allowedRegistrationDomains == null || allowedRegistrationDomains.isEmpty()) {
            return true;
        }
        if (email == null || !email.contains("@")) {
            return false;
        }
        String domain = email.substring(email.lastIndexOf('@') + 1).toLowerCase();
        return allowedRegistrationDomains.stream()
                .map(d -> d.trim().toLowerCase())
                .filter(d -> !d.isEmpty())
                .anyMatch(d -> domain.equals(d) || domain.endsWith("." + d));
    }
}
