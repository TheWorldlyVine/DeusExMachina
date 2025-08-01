package com.deusexmachina.auth.domain;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;

import java.util.Map;
import java.util.Objects;

/**
 * Domain entity representing a permission grant.
 * Immutable value object for permission data.
 */
public class Permission {
    @DocumentId
    private final String permissionId;
    private final String resourceId;
    private final ResourceType resourceType;
    private final String grantedTo;      // User ID
    private final String grantedBy;      // User ID who granted
    private final PermissionLevel level;
    @ServerTimestamp
    private final Timestamp grantedAt;
    private final Timestamp expiresAt;   // Optional expiration
    private final Map<String, Boolean> customPermissions; // For future expansion

    private Permission(Builder builder) {
        this.permissionId = Objects.requireNonNull(builder.permissionId, "permissionId cannot be null");
        this.resourceId = Objects.requireNonNull(builder.resourceId, "resourceId cannot be null");
        this.resourceType = Objects.requireNonNull(builder.resourceType, "resourceType cannot be null");
        this.grantedTo = Objects.requireNonNull(builder.grantedTo, "grantedTo cannot be null");
        this.grantedBy = Objects.requireNonNull(builder.grantedBy, "grantedBy cannot be null");
        this.level = Objects.requireNonNull(builder.level, "level cannot be null");
        this.grantedAt = builder.grantedAt;
        this.expiresAt = builder.expiresAt;
        this.customPermissions = builder.customPermissions;
    }

    // Getters
    public String getPermissionId() { return permissionId; }
    public String getResourceId() { return resourceId; }
    public ResourceType getResourceType() { return resourceType; }
    public String getGrantedTo() { return grantedTo; }
    public String getGrantedBy() { return grantedBy; }
    public PermissionLevel getLevel() { return level; }
    public Timestamp getGrantedAt() { return grantedAt; }
    public Timestamp getExpiresAt() { return expiresAt; }
    public Map<String, Boolean> getCustomPermissions() { return customPermissions; }

    // Domain methods
    public boolean isExpired() {
        if (expiresAt == null) return false;
        return expiresAt.toDate().before(new java.util.Date());
    }

    public boolean hasPermission(String action) {
        // Check standard permissions based on level
        switch (level) {
            case OWNER:
                return true; // Owners can do everything
            case EDITOR:
                return !action.equals("delete") && !action.equals("share");
            case VIEWER:
                return action.equals("read");
            default:
                return false;
        }
    }

    public boolean hasCustomPermission(String permission) {
        if (customPermissions == null) return false;
        return customPermissions.getOrDefault(permission, false);
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String permissionId;
        private String resourceId;
        private ResourceType resourceType;
        private String grantedTo;
        private String grantedBy;
        private PermissionLevel level;
        private Timestamp grantedAt;
        private Timestamp expiresAt;
        private Map<String, Boolean> customPermissions;

        public Builder permissionId(String permissionId) {
            this.permissionId = permissionId;
            return this;
        }

        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder resourceType(ResourceType resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder grantedTo(String grantedTo) {
            this.grantedTo = grantedTo;
            return this;
        }

        public Builder grantedBy(String grantedBy) {
            this.grantedBy = grantedBy;
            return this;
        }

        public Builder level(PermissionLevel level) {
            this.level = level;
            return this;
        }

        public Builder grantedAt(Timestamp grantedAt) {
            this.grantedAt = grantedAt;
            return this;
        }

        public Builder expiresAt(Timestamp expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder customPermissions(Map<String, Boolean> customPermissions) {
            this.customPermissions = customPermissions;
            return this;
        }

        public Permission build() {
            return new Permission(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return Objects.equals(permissionId, that.permissionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permissionId);
    }
}