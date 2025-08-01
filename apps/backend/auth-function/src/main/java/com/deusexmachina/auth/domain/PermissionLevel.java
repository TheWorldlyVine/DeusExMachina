package com.deusexmachina.auth.domain;

/**
 * Enum representing permission levels in the RBAC system.
 * Follows a hierarchical structure: OWNER > EDITOR > VIEWER
 */
public enum PermissionLevel {
    OWNER("owner", 3),
    EDITOR("editor", 2),
    VIEWER("viewer", 1);

    private final String value;
    private final int priority;

    PermissionLevel(String value, int priority) {
        this.value = value;
        this.priority = priority;
    }

    public String getValue() {
        return value;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * Check if this level has higher or equal privileges than another.
     */
    public boolean hasPrivilegesOf(PermissionLevel other) {
        return this.priority >= other.priority;
    }

    public static PermissionLevel fromValue(String value) {
        for (PermissionLevel level : PermissionLevel.values()) {
            if (level.value.equalsIgnoreCase(value)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown permission level: " + value);
    }
}