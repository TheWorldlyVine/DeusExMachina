package com.deusexmachina.auth.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for token refresh response.
 */
public record TokenResponse(
    @SerializedName("access_token")
    String accessToken,
    
    @SerializedName("refresh_token")
    String refreshToken,
    
    @SerializedName("expires_in")
    long expiresIn,
    
    @SerializedName("token_type")
    String tokenType
) {
    public TokenResponse {
        tokenType = tokenType != null ? tokenType : "Bearer";
    }
}