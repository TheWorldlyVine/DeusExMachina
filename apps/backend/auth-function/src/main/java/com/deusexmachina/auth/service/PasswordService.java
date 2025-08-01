package com.deusexmachina.auth.service;

/**
 * Service interface for password operations.
 * Single Responsibility: Handle password hashing and validation.
 */
public interface PasswordService {
    /**
     * Hash a password using Argon2id.
     */
    String hashPassword(String password);
    
    /**
     * Verify a password against a hash.
     */
    boolean verifyPassword(String password, String hash);
    
    /**
     * Validate password strength.
     * @return true if password meets strength requirements
     */
    boolean validatePasswordStrength(String password);
    
    /**
     * Check if password has been breached.
     * @return true if password is found in breach database
     */
    boolean isPasswordBreached(String password);
    
    /**
     * Generate a secure random password.
     */
    String generateSecurePassword();
}