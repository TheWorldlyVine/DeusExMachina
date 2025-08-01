package com.deusexmachina.shared.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for common validation operations.
 * Provides methods for validating data formats, ranges, and business rules.
 */
public final class ValidationUtils {
    
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();
    
    // Common regex patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$"
    );
    
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9]+$"
    );
    
    private ValidationUtils() {
        // Prevent instantiation
    }
    
    /**
     * Validates an object using Jakarta Bean Validation.
     * 
     * @param object the object to validate
     * @param <T> the type of the object
     * @return set of constraint violations, empty if valid
     */
    public static <T> Set<ConstraintViolation<T>> validate(T object) {
        return VALIDATOR.validate(object);
    }
    
    /**
     * Validates an object and returns error messages.
     * 
     * @param object the object to validate
     * @param <T> the type of the object
     * @return concatenated error messages, null if valid
     */
    public static <T> String validateAndGetErrors(T object) {
        Set<ConstraintViolation<T>> violations = validate(object);
        if (violations.isEmpty()) {
            return null;
        }
        
        return violations.stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.joining(", "));
    }
    
    /**
     * Checks if a string is a valid email address.
     * 
     * @param email the email to validate
     * @return true if valid email, false otherwise
     */
    public static boolean isValidEmail(String email) {
        return StringUtils.isNotBlank(email) && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Checks if a string is a valid phone number (E.164 format).
     * 
     * @param phone the phone number to validate
     * @return true if valid phone number, false otherwise
     */
    public static boolean isValidPhoneNumber(String phone) {
        return StringUtils.isNotBlank(phone) && PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * Checks if a string is a valid UUID.
     * 
     * @param uuid the UUID to validate
     * @return true if valid UUID, false otherwise
     */
    public static boolean isValidUUID(String uuid) {
        return StringUtils.isNotBlank(uuid) && UUID_PATTERN.matcher(uuid).matches();
    }
    
    /**
     * Checks if a string contains only alphanumeric characters.
     * 
     * @param text the text to validate
     * @return true if alphanumeric, false otherwise
     */
    public static boolean isAlphanumeric(String text) {
        return StringUtils.isNotBlank(text) && ALPHANUMERIC_PATTERN.matcher(text).matches();
    }
    
    /**
     * Validates that a string is within specified length bounds.
     * 
     * @param text the text to validate
     * @param minLength minimum length (inclusive)
     * @param maxLength maximum length (inclusive)
     * @return true if within bounds, false otherwise
     */
    public static boolean isValidLength(String text, int minLength, int maxLength) {
        if (text == null) {
            return minLength == 0;
        }
        int length = text.length();
        return length >= minLength && length <= maxLength;
    }
    
    /**
     * Validates that a number is within specified range.
     * 
     * @param value the value to validate
     * @param min minimum value (inclusive)
     * @param max maximum value (inclusive)
     * @return true if within range, false otherwise
     */
    public static boolean isInRange(long value, long min, long max) {
        return value >= min && value <= max;
    }
    
    /**
     * Validates that a number is within specified range.
     * 
     * @param value the value to validate
     * @param min minimum value (inclusive)
     * @param max maximum value (inclusive)
     * @return true if within range, false otherwise
     */
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }
    
    /**
     * Sanitizes a string by removing potentially harmful characters.
     * Useful for preventing XSS and injection attacks.
     * 
     * @param input the input string
     * @return sanitized string
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove HTML tags
        String sanitized = input.replaceAll("<[^>]*>", "");
        
        // Escape special characters
        sanitized = sanitized
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");
        
        return sanitized.trim();
    }
}