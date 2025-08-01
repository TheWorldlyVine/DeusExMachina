package com.deusexmachina.auth.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing a user session.
 * Immutable value object for session data.
 */
public class Session {
    private final String sessionId;
    private final String userId;
    private final String refreshTokenHash;
    private final DeviceInfo deviceInfo;
    private final String ipAddress;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Instant lastAccessedAt;
    private final Instant revokedAt;

    private Session(Builder builder) {
        this.sessionId = Objects.requireNonNull(builder.sessionId, "sessionId cannot be null");
        this.userId = Objects.requireNonNull(builder.userId, "userId cannot be null");
        this.refreshTokenHash = Objects.requireNonNull(builder.refreshTokenHash, "refreshTokenHash cannot be null");
        this.deviceInfo = builder.deviceInfo;
        this.ipAddress = builder.ipAddress;
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt cannot be null");
        this.expiresAt = Objects.requireNonNull(builder.expiresAt, "expiresAt cannot be null");
        this.lastAccessedAt = builder.lastAccessedAt;
        this.revokedAt = builder.revokedAt;
    }

    // Getters
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public String getRefreshTokenHash() { return refreshTokenHash; }
    public DeviceInfo getDeviceInfo() { return deviceInfo; }
    public String getIpAddress() { return ipAddress; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getLastAccessedAt() { return lastAccessedAt; }
    public Instant getRevokedAt() { return revokedAt; }

    // Domain methods
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isActive() {
        return !isExpired() && !isRevoked();
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .sessionId(this.sessionId)
                .userId(this.userId)
                .refreshTokenHash(this.refreshTokenHash)
                .deviceInfo(this.deviceInfo)
                .ipAddress(this.ipAddress)
                .createdAt(this.createdAt)
                .expiresAt(this.expiresAt)
                .lastAccessedAt(this.lastAccessedAt)
                .revokedAt(this.revokedAt);
    }

    public static class Builder {
        private String sessionId;
        private String userId;
        private String refreshTokenHash;
        private DeviceInfo deviceInfo;
        private String ipAddress;
        private Instant createdAt;
        private Instant expiresAt;
        private Instant lastAccessedAt;
        private Instant revokedAt;

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder refreshTokenHash(String refreshTokenHash) {
            this.refreshTokenHash = refreshTokenHash;
            return this;
        }

        public Builder deviceInfo(DeviceInfo deviceInfo) {
            this.deviceInfo = deviceInfo;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder lastAccessedAt(Instant lastAccessedAt) {
            this.lastAccessedAt = lastAccessedAt;
            return this;
        }

        public Builder revokedAt(Instant revokedAt) {
            this.revokedAt = revokedAt;
            return this;
        }

        public Session build() {
            return new Session(this);
        }
    }

    /**
     * Value object for device information.
     */
    public static class DeviceInfo {
        private final String userAgent;
        private final String deviceType;
        private final String browser;
        private final String os;

        public DeviceInfo(String userAgent, String deviceType, String browser, String os) {
            this.userAgent = userAgent;
            this.deviceType = deviceType;
            this.browser = browser;
            this.os = os;
        }

        // Getters
        public String getUserAgent() { return userAgent; }
        public String getDeviceType() { return deviceType; }
        public String getBrowser() { return browser; }
        public String getOs() { return os; }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return Objects.equals(sessionId, session.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }
}