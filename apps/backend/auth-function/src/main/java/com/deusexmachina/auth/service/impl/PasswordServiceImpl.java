package com.deusexmachina.auth.service.impl;

import com.deusexmachina.auth.service.PasswordService;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Implementation of PasswordService using Argon2id.
 * Follows Single Responsibility Principle - only handles password operations.
 */
@Singleton
public class PasswordServiceImpl implements PasswordService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordServiceImpl.class);
    
    private final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
    private final SecureRandom secureRandom = new SecureRandom();
    
    // Argon2 parameters (following OWASP recommendations)
    private static final int MEMORY_COST = 65536; // 64MB
    private static final int TIME_COST = 3;
    private static final int PARALLELISM = 4;
    
    // Password validation patterns
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");
    
    @Override
    public String hashPassword(String password) {
        try {
            return argon2.hash(TIME_COST, MEMORY_COST, PARALLELISM, 
                    password.toCharArray());
        } finally {
            // Clear password from memory
            argon2.wipeArray(password.toCharArray());
        }
    }
    
    @Override
    public boolean verifyPassword(String password, String hash) {
        try {
            return argon2.verify(hash, password.toCharArray());
        } finally {
            // Clear password from memory
            argon2.wipeArray(password.toCharArray());
        }
    }
    
    @Override
    public boolean validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        // Must contain at least one of each: uppercase, lowercase, digit, special char
        return UPPERCASE.matcher(password).find() &&
               LOWERCASE.matcher(password).find() &&
               DIGIT.matcher(password).find() &&
               SPECIAL.matcher(password).find();
    }
    
    @Override
    public boolean isPasswordBreached(String password) {
        try {
            // Calculate SHA-1 hash of password (used by HaveIBeenPwned API)
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = sha1.digest(password.getBytes(StandardCharsets.UTF_8));
            String hash = bytesToHex(hashBytes);
            
            // TODO: Implement actual HaveIBeenPwned API check
            // For now, just check against common passwords
            return isCommonPassword(password);
            
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to calculate SHA-1 hash", e);
            return false;
        }
    }
    
    @Override
    public String generateSecurePassword() {
        // Generate a 16-character password with guaranteed complexity
        StringBuilder password = new StringBuilder();
        
        // Ensure at least one of each required character type
        password.append(generateRandomChar("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
        password.append(generateRandomChar("abcdefghijklmnopqrstuvwxyz"));
        password.append(generateRandomChar("0123456789"));
        password.append(generateRandomChar("!@#$%^&*()"));
        
        // Fill remaining characters randomly
        String allChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
        for (int i = 4; i < 16; i++) {
            password.append(generateRandomChar(allChars));
        }
        
        // Shuffle the password
        return shuffleString(password.toString());
    }
    
    private char generateRandomChar(String chars) {
        return chars.charAt(secureRandom.nextInt(chars.length()));
    }
    
    private String shuffleString(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    private boolean isCommonPassword(String password) {
        // Common passwords to check against
        String[] commonPasswords = {
            "password", "12345678", "password123", "admin", "letmein",
            "welcome", "monkey", "dragon", "baseball", "iloveyou"
        };
        
        String lowerPassword = password.toLowerCase();
        for (String common : commonPasswords) {
            if (lowerPassword.equals(common)) {
                return true;
            }
        }
        return false;
    }
}