package com.deusexmachina.email.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum of all email types in the system.
 * This is designed to be extensible - add new email types here as needed.
 */
@Getter
@RequiredArgsConstructor
public enum EmailType {
    // Authentication emails
    VERIFICATION_EMAIL("Verify your email address", "auth"),
    PASSWORD_RESET("Reset your password", "auth"),
    PASSWORD_CHANGED("Your password has been changed", "auth"),
    NEW_DEVICE_LOGIN("New device login detected", "auth"),
    MFA_CODE("Your verification code", "auth"),
    ACCOUNT_LOCKED("Account security alert", "auth"),
    
    // Transaction emails (future)
    ORDER_CONFIRMATION("Order confirmation", "transaction"),
    PAYMENT_RECEIPT("Payment receipt", "transaction"),
    INVOICE("Invoice", "transaction"),
    
    // Notification emails (future)
    WELCOME("Welcome to DeusExMachina", "notification"),
    ACTIVITY_SUMMARY("Your activity summary", "notification"),
    SYSTEM_ANNOUNCEMENT("Important announcement", "notification"),
    
    // Marketing emails (future)
    NEWSLETTER("Newsletter", "marketing"),
    PRODUCT_UPDATE("Product updates", "marketing"),
    PROMOTIONAL("Special offer", "marketing");
    
    private final String defaultSubject;
    private final String category;
    
    /**
     * Check if this email type requires immediate delivery
     */
    public boolean isHighPriority() {
        return switch (this) {
            case VERIFICATION_EMAIL, PASSWORD_RESET, MFA_CODE, ACCOUNT_LOCKED -> true;
            default -> false;
        };
    }
    
    /**
     * Check if this email type contains sensitive information
     */
    public boolean isSensitive() {
        return category.equals("auth") || category.equals("transaction");
    }
    
    /**
     * Get retention period in days for this email type
     */
    public int getRetentionDays() {
        return switch (category) {
            case "auth", "transaction" -> 90; // 3 months
            case "notification" -> 30; // 1 month
            case "marketing" -> 7; // 1 week
            default -> 30;
        };
    }
}