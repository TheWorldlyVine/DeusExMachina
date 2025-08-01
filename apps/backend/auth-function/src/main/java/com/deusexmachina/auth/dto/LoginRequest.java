package com.deusexmachina.auth.dto;

import jakarta.validation.constraints.*;

/**
 * DTO for login request.
 */
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Password is required")
    String password,
    
    Boolean rememberMe
) {
    public LoginRequest {
        // Default rememberMe to false if null
        rememberMe = rememberMe != null ? rememberMe : false;
    }
}