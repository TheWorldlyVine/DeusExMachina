package com.deusexmachina.auth.domain;

import com.google.cloud.Timestamp;

/**
 * Value object representing user security settings.
 */
public class UserSecuritySettings {
    private final boolean mfaEnabled;
    private final String mfaSecret;
    private final Timestamp lastPasswordChange;
    private final int failedLoginAttempts;
    private final Timestamp lockoutUntil;

    public UserSecuritySettings() {
        this(false, null, null, 0, null);
    }

    public UserSecuritySettings(boolean mfaEnabled, String mfaSecret, 
                              Timestamp lastPasswordChange, int failedLoginAttempts, 
                              Timestamp lockoutUntil) {
        this.mfaEnabled = mfaEnabled;
        this.mfaSecret = mfaSecret;
        this.lastPasswordChange = lastPasswordChange;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockoutUntil = lockoutUntil;
    }

    // Getters
    public boolean isMfaEnabled() { return mfaEnabled; }
    public String getMfaSecret() { return mfaSecret; }
    public Timestamp getLastPasswordChange() { return lastPasswordChange; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Timestamp getLockoutUntil() { return lockoutUntil; }

    // Domain logic
    public boolean isLockedOut() {
        if (lockoutUntil == null) return false;
        return lockoutUntil.toDate().after(new java.util.Date());
    }

    // Builder for immutability
    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .mfaEnabled(this.mfaEnabled)
                .mfaSecret(this.mfaSecret)
                .lastPasswordChange(this.lastPasswordChange)
                .failedLoginAttempts(this.failedLoginAttempts)
                .lockoutUntil(this.lockoutUntil);
    }

    public static class Builder {
        private boolean mfaEnabled = false;
        private String mfaSecret;
        private Timestamp lastPasswordChange;
        private int failedLoginAttempts = 0;
        private Timestamp lockoutUntil;

        public Builder mfaEnabled(boolean mfaEnabled) {
            this.mfaEnabled = mfaEnabled;
            return this;
        }

        public Builder mfaSecret(String mfaSecret) {
            this.mfaSecret = mfaSecret;
            return this;
        }

        public Builder lastPasswordChange(Timestamp lastPasswordChange) {
            this.lastPasswordChange = lastPasswordChange;
            return this;
        }

        public Builder failedLoginAttempts(int failedLoginAttempts) {
            this.failedLoginAttempts = failedLoginAttempts;
            return this;
        }

        public Builder lockoutUntil(Timestamp lockoutUntil) {
            this.lockoutUntil = lockoutUntil;
            return this;
        }

        public UserSecuritySettings build() {
            return new UserSecuritySettings(mfaEnabled, mfaSecret, 
                    lastPasswordChange, failedLoginAttempts, lockoutUntil);
        }
    }
}