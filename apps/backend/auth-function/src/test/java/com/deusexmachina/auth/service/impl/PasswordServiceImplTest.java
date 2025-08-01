package com.deusexmachina.auth.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PasswordServiceImpl.
 * Tests password hashing, validation, and strength checking.
 */
class PasswordServiceImplTest {
    
    private PasswordServiceImpl passwordService;
    
    @BeforeEach
    void setUp() {
        passwordService = new PasswordServiceImpl();
    }
    
    @Test
    void testHashPassword_ShouldReturnValidArgon2Hash() {
        String password = "SecurePassword123!";
        String hash = passwordService.hashPassword(password);
        
        assertNotNull(hash);
        assertTrue(hash.startsWith("$argon2id$"));
        assertTrue(hash.length() > 50);
    }
    
    @Test
    void testHashPassword_SamePasswordDifferentHashes() {
        String password = "SecurePassword123!";
        String hash1 = passwordService.hashPassword(password);
        String hash2 = passwordService.hashPassword(password);
        
        assertNotEquals(hash1, hash2, "Same password should produce different hashes due to salt");
    }
    
    @Test
    void testVerifyPassword_CorrectPassword() {
        String password = "SecurePassword123!";
        String hash = passwordService.hashPassword(password);
        
        assertTrue(passwordService.verifyPassword(password, hash));
    }
    
    @Test
    void testVerifyPassword_IncorrectPassword() {
        String password = "SecurePassword123!";
        String hash = passwordService.hashPassword(password);
        
        assertFalse(passwordService.verifyPassword("WrongPassword123!", hash));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "SecurePass123!",     // Valid password
        "MyP@ssw0rd2023",     // Valid password
        "C0mpl3x!Pass",       // Valid password
    })
    void testValidatePasswordStrength_ValidPasswords(String password) {
        assertTrue(passwordService.validatePasswordStrength(password));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "password",           // No uppercase, digits, or special chars
        "Password",           // No digits or special chars
        "Password123",        // No special chars
        "password123!",       // No uppercase
        "PASSWORD123!",       // No lowercase
        "Pass!",              // Too short
        "",                   // Empty
    })
    void testValidatePasswordStrength_InvalidPasswords(String password) {
        assertFalse(passwordService.validatePasswordStrength(password));
    }
    
    @Test
    void testValidatePasswordStrength_NullPassword() {
        assertFalse(passwordService.validatePasswordStrength(null));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "password",
        "12345678",
        "password123",
        "admin",
        "letmein"
    })
    void testIsPasswordBreached_CommonPasswords(String password) {
        assertTrue(passwordService.isPasswordBreached(password));
    }
    
    @Test
    void testIsPasswordBreached_UniquePassword() {
        String uniquePassword = "Xk9#mP2$vL7@nQ4*";
        assertFalse(passwordService.isPasswordBreached(uniquePassword));
    }
    
    @Test
    void testGenerateSecurePassword() {
        String password = passwordService.generateSecurePassword();
        
        assertNotNull(password);
        assertEquals(16, password.length());
        assertTrue(passwordService.validatePasswordStrength(password));
        
        // Verify it contains all required character types
        assertTrue(password.matches(".*[A-Z].*"));
        assertTrue(password.matches(".*[a-z].*"));
        assertTrue(password.matches(".*[0-9].*"));
        assertTrue(password.matches(".*[!@#$%^&*()].*"));
    }
    
    @Test
    void testGenerateSecurePassword_UniquePasswords() {
        String password1 = passwordService.generateSecurePassword();
        String password2 = passwordService.generateSecurePassword();
        
        assertNotEquals(password1, password2);
    }
}