package com.deusexmachina.shared.constants;

/**
 * Centralized error codes for the DeusExMachina platform.
 * These codes are used across all services for consistent error handling.
 */
public final class ErrorCodes {
    
    // Authentication errors (1000-1999)
    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_1001";
    public static final String AUTH_TOKEN_EXPIRED = "AUTH_1002";
    public static final String AUTH_TOKEN_INVALID = "AUTH_1003";
    public static final String AUTH_INSUFFICIENT_PERMISSIONS = "AUTH_1004";
    public static final String AUTH_USER_NOT_FOUND = "AUTH_1005";
    public static final String AUTH_USER_DISABLED = "AUTH_1006";
    
    // Validation errors (2000-2999)
    public static final String VALIDATION_REQUIRED_FIELD = "VAL_2001";
    public static final String VALIDATION_INVALID_FORMAT = "VAL_2002";
    public static final String VALIDATION_OUT_OF_RANGE = "VAL_2003";
    public static final String VALIDATION_DUPLICATE_ENTRY = "VAL_2004";
    
    // Business logic errors (3000-3999)
    public static final String BUSINESS_RULE_VIOLATION = "BIZ_3001";
    public static final String RESOURCE_NOT_FOUND = "BIZ_3002";
    public static final String OPERATION_NOT_ALLOWED = "BIZ_3003";
    public static final String QUOTA_EXCEEDED = "BIZ_3004";
    
    // System errors (5000-5999)
    public static final String SYSTEM_INTERNAL_ERROR = "SYS_5001";
    public static final String SYSTEM_SERVICE_UNAVAILABLE = "SYS_5002";
    public static final String SYSTEM_TIMEOUT = "SYS_5003";
    public static final String SYSTEM_RATE_LIMIT = "SYS_5004";
    
    private ErrorCodes() {
        // Prevent instantiation
    }
}