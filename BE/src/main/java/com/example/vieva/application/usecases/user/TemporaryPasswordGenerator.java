package com.example.vieva.application.usecases.user;

import java.security.SecureRandom;

/**
 * Generates the temporary password for an admin-created account when the admin does not supply one.
 * Ambiguous characters (0/O, 1/l/I) are left out because users type it from an email.
 */
final class TemporaryPasswordGenerator {

    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String ALPHABET = LETTERS + DIGITS;
    private static final int LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswordGenerator() {
    }

    static String generate() {
        char[] password = new char[LENGTH];
        password[0] = LETTERS.charAt(RANDOM.nextInt(LETTERS.length()));
        password[1] = DIGITS.charAt(RANDOM.nextInt(DIGITS.length()));
        for (int i = 2; i < LENGTH; i++) {
            password[i] = ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length()));
        }
        // Shuffle so the guaranteed letter and digit are not always first
        for (int i = LENGTH - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = password[i];
            password[i] = password[j];
            password[j] = tmp;
        }
        return new String(password);
    }
}
