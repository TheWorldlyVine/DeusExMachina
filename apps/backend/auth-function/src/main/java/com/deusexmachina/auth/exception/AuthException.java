package com.deusexmachina.auth.exception;

/**
 * Custom exception for authentication errors.
 */
public class AuthException extends RuntimeException {
    private final int statusCode;
    
    public AuthException(String message) {
        this(message, 401);
    }
    
    public AuthException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public AuthException(String message, Throwable cause) {
        this(message, cause, 401);
    }
    
    public AuthException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}