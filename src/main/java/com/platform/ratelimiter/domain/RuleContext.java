package com.platform.ratelimiter.domain;

import java.time.Instant;
import java.util.Objects;

public record RuleContext(
        String tenantId,
        String userId,
        String apiPath,
        UserTier userTier,
        String correlationId,
        Instant requestTime
) {
    public RuleContext {
        tenantId = normalize(tenantId, "anonymous-tenant");
        userId = normalize(userId, "anonymous-user");
        apiPath = normalize(apiPath, "/unknown");
        userTier = Objects.requireNonNullElse(userTier, UserTier.FREE);
        correlationId = normalize(correlationId, "missing-correlation-id");
        requestTime = Objects.requireNonNullElseGet(requestTime, Instant::now);
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
