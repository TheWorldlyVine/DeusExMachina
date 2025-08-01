package com.deusexmachina.auth.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for authentication response.
 */
public record AuthResponse(
    @SerializedName("user_id")
    String userId,
    
    @SerializedName("access_token")
    String accessToken,
    
    @SerializedName("refresh_token")
    String refreshToken,
    
    @SerializedName("expires_in")
    long expiresIn,
    
    @SerializedName("token_type")
    String tokenType,
    
    @SerializedName("user")
    UserInfo user
) {
    public AuthResponse {
        tokenType = tokenType != null ? tokenType : "Bearer";
    }
    
    /**
     * Nested user info for response.
     */
    public record UserInfo(
        String email,
        @SerializedName("display_name")
        String displayName,
        @SerializedName("email_verified")
        boolean emailVerified,
        @SerializedName("auth_provider")
        String authProvider
    ) {}
}