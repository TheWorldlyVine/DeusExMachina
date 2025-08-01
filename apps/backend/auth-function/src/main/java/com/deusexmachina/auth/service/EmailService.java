package com.deusexmachina.auth.service;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for email operations.
 * Single Responsibility: Handle email sending for auth flows.
 */
public interface EmailService {
    /**
     * Send email verification.
     */
    CompletableFuture<Void> sendVerificationEmail(String email, String verificationToken);
    
    /**
     * Send password reset email.
     */
    CompletableFuture<Void> sendPasswordResetEmail(String email, String resetToken);
    
    /**
     * Send login notification for new device.
     */
    CompletableFuture<Void> sendNewDeviceLoginNotification(String email, String deviceInfo, String ipAddress);
    
    /**
     * Send MFA code via email.
     */
    CompletableFuture<Void> sendMfaCode(String email, String code);
    
    /**
     * Send account locked notification.
     */
    CompletableFuture<Void> sendAccountLockedNotification(String email, int attempts);
}