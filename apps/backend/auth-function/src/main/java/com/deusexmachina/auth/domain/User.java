package com.deusexmachina.auth.domain;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Domain entity representing a user.
 * This is an immutable value object following DDD principles.
 */
public class User {
    @DocumentId
    private final String userId;
    private final String email;
    private final String passwordHash;
    private final String displayName;
    private final AuthProvider authProvider;
    private final boolean emailVerified;
    @ServerTimestamp
    private final Timestamp createdAt;
    @ServerTimestamp
    private final Timestamp updatedAt;
    private final List<String> linkedProviders;
    private final UserSecuritySettings securitySettings;

    private User(Builder builder) {
        this.userId = Objects.requireNonNull(builder.userId, "userId cannot be null");
        this.email = Objects.requireNonNull(builder.email, "email cannot be null");
        this.passwordHash = builder.passwordHash; // Can be null for OAuth users
        this.displayName = builder.displayName;
        this.authProvider = Objects.requireNonNull(builder.authProvider, "authProvider cannot be null");
        this.emailVerified = builder.emailVerified;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.linkedProviders = new ArrayList<>(builder.linkedProviders);
        this.securitySettings = builder.securitySettings != null ? builder.securitySettings : new UserSecuritySettings();
    }

    // Getters
    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public AuthProvider getAuthProvider() { return authProvider; }
    public boolean isEmailVerified() { return emailVerified; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public List<String> getLinkedProviders() { return new ArrayList<>(linkedProviders); }
    public UserSecuritySettings getSecuritySettings() { return securitySettings; }

    // Domain methods
    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isEmpty();
    }

    public boolean isProviderLinked(String provider) {
        return linkedProviders.contains(provider);
    }

    // Builder pattern for immutability
    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .userId(this.userId)
                .email(this.email)
                .passwordHash(this.passwordHash)
                .displayName(this.displayName)
                .authProvider(this.authProvider)
                .emailVerified(this.emailVerified)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .linkedProviders(this.linkedProviders)
                .securitySettings(this.securitySettings);
    }

    public static class Builder {
        private String userId;
        private String email;
        private String passwordHash;
        private String displayName;
        private AuthProvider authProvider;
        private boolean emailVerified = false;
        private Timestamp createdAt;
        private Timestamp updatedAt;
        private List<String> linkedProviders = new ArrayList<>();
        private UserSecuritySettings securitySettings;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder authProvider(AuthProvider authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        public Builder emailVerified(boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public Builder createdAt(Timestamp createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Timestamp updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder linkedProviders(List<String> linkedProviders) {
            this.linkedProviders = new ArrayList<>(linkedProviders);
            return this;
        }

        public Builder addLinkedProvider(String provider) {
            this.linkedProviders.add(provider);
            return this;
        }

        public Builder securitySettings(UserSecuritySettings securitySettings) {
            this.securitySettings = securitySettings;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}