package com.deusexmachina.auth.domain;

/**
 * Enum representing types of resources that can have permissions.
 */
public enum ResourceType {
    WORLD("world"),
    CHARACTER("character"),
    LOCATION("location"),
    ITEM("item"),
    STORY("story"),
    CAMPAIGN("campaign"),
    WORKSPACE("workspace");

    private final String value;

    ResourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ResourceType fromValue(String value) {
        for (ResourceType type : ResourceType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown resource type: " + value);
    }
}