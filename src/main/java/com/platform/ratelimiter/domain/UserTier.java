package com.platform.ratelimiter.domain;

public enum UserTier {
    FREE,
    PREMIUM;

    public static UserTier fromHeader(String value) {
        if (value == null || value.isBlank()) {
            return FREE;
        }
        try {
            return UserTier.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return FREE;
        }
    }
}
