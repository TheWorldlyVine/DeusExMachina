package com.deusexmachina.email.processor.model;

import lombok.Getter;

/**
 * Enum representing different types of emails that can be sent.
 */
@Getter
public enum EmailType {
    VERIFICATION_EMAIL("Verify your email address", true),
    PASSWORD_RESET("Reset your password", true),
    PASSWORD_CHANGED("Your password has been changed", false),
    NEW_DEVICE_LOGIN("New device login detected", false),
    MFA_CODE("Your verification code", true),
    ACCOUNT_LOCKED("Security alert: Account locked", true),
    WELCOME_EMAIL("Welcome to DeusExMachina", false),
    ORDER_CONFIRMATION("Order confirmation", false),
    PAYMENT_RECEIVED("Payment received", false),
    SUBSCRIPTION_RENEWAL("Subscription renewal", false);
    
    private final String defaultSubject;
    private final boolean highPriority;
    
    EmailType(String defaultSubject, boolean highPriority) {
        this.defaultSubject = defaultSubject;
        this.highPriority = highPriority;
    }
}