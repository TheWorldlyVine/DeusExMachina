package com.deusexmachina.auth.service;

import com.deusexmachina.auth.domain.User;
import com.deusexmachina.auth.domain.Session;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Map;

/**
 * Service interface for JWT token operations.
 * Single Responsibility: Handle JWT token generation and validation.
 */
public interface TokenService {
    /**
     * Generate access token for user.
     */
    String generateAccessToken(User user, Map<String, Object> additionalClaims);
    
    /**
     * Generate refresh token for session.
     */
    String generateRefreshToken(Session session);
    
    /**
     * Verify and decode access token.
     */
    DecodedJWT verifyAccessToken(String token);
    
    /**
     * Verify and decode refresh token.
     */
    DecodedJWT verifyRefreshToken(String token);
    
    /**
     * Extract user ID from token.
     */
    String extractUserId(String token);
    
    /**
     * Hash a refresh token for storage.
     */
    String hashToken(String token);
}