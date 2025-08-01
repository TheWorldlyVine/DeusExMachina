package com.deusexmachina.auth.domain;

/**
 * Enum representing supported authentication providers.
 */
public enum AuthProvider {
    EMAIL("email"),
    GOOGLE("google"),
    FACEBOOK("facebook"),
    APPLE("apple"),
    DISCORD("discord");

    private final String value;

    AuthProvider(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AuthProvider fromValue(String value) {
        for (AuthProvider provider : AuthProvider.values()) {
            if (provider.value.equalsIgnoreCase(value)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown auth provider: " + value);
    }
}