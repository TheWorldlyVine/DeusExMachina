package com.deusexmachina.auth.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for generating secure random tokens.
 */
public final class TokenGenerator {
    private static final SecureRandom secureRandom = new SecureRandom();
    
    private TokenGenerator() {
        // Utility class
    }
    
    /**
     * Generate a secure random token.
     * @return Base64 URL-safe encoded token
     */
    public static String generateSecureToken() {
        byte[] tokenBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
    
    /**
     * Generate a numeric code (for MFA, etc).
     * @param digits Number of digits
     * @return Numeric code as string
     */
    public static String generateNumericCode(int digits) {
        if (digits <= 0 || digits > 10) {
            throw new IllegalArgumentException("Digits must be between 1 and 10");
        }
        
        int bound = (int) Math.pow(10, digits);
        int code = secureRandom.nextInt(bound);
        return String.format("%0" + digits + "d", code);
    }
}