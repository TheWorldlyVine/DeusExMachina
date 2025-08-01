package com.deusexmachina.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for Google OAuth login request.
 */
public record GoogleLoginRequest(
    @NotBlank(message = "ID token is required")
    String idToken
) {}