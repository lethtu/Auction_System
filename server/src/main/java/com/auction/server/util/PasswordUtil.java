package com.auction.server.util;

import org.springframework.security.crypto.bcrypt.BCrypt;

public class PasswordUtil {

    /**
     * Hashes a plaintext password using BCrypt.
     */
    public static String hashPassword(String plainTextPassword) {
        if (plainTextPassword == null) {
            return null;
        }
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(12));
    }

    /**
     * Checks if a plaintext password matches a stored password.
     * Supports checking against both BCrypt hashed passwords and legacy plaintext passwords.
     */
    public static boolean checkPassword(String plainTextPassword, String storedPassword) {
        if (plainTextPassword == null || storedPassword == null) {
            return false;
        }
        // Check if the stored password looks like a BCrypt hash
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            try {
                return BCrypt.checkpw(plainTextPassword, storedPassword);
            } catch (Exception e) {
                // If checking fails (e.g. invalid BCrypt format), fallback to plain comparison
                return plainTextPassword.equals(storedPassword);
            }
        }
        // Fallback for legacy plaintext password comparison
        return plainTextPassword.equals(storedPassword);
    }

    /**
     * Check if a stored password is in plaintext format (not hashed with BCrypt).
     */
    public static boolean isPlaintext(String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        return !(storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$"));
    }
}
