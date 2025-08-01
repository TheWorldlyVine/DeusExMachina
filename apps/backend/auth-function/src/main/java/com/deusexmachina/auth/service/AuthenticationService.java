package com.deusexmachina.auth.service;

import com.deusexmachina.auth.dto.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for authentication operations.
 * Single Responsibility: Handle user authentication flows.
 */
public interface AuthenticationService {
    /**
     * Register a new user with email and password.
     */
    CompletableFuture<AuthResponse> register(RegisterRequest request);
    
    /**
     * Authenticate user with email and password.
     */
    CompletableFuture<AuthResponse> login(LoginRequest request);
    
    /**
     * Authenticate user with Google OAuth.
     */
    CompletableFuture<AuthResponse> googleLogin(GoogleLoginRequest request);
    
    /**
     * Refresh access token using refresh token.
     */
    CompletableFuture<TokenResponse> refreshToken(String refreshToken);
    
    /**
     * Logout user and revoke session.
     */
    CompletableFuture<Void> logout(String refreshToken, boolean logoutAll);
    
    /**
     * Verify email with verification token.
     */
    CompletableFuture<Void> verifyEmail(String token);
    
    /**
     * Initiate password reset.
     */
    CompletableFuture<Void> initiatePasswordReset(String email);
    
    /**
     * Complete password reset with token.
     */
    CompletableFuture<Void> completePasswordReset(String token, String newPassword);
}