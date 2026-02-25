package com.platform.ratelimiter.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record RateLimitRule(
        String ruleId,
        ScopeType scopeType,
        String scopeValue,
        AlgorithmType algorithmType,
        long limit,
        long burstCapacity,
        Duration window,
        EnforcementMode enforcementMode,
        Set<UserTier> appliesToTiers,
        int priority,
        boolean enabled,
        Instant updatedAt
) {
    private static final String WILDCARD = "*";

    public RateLimitRule {
        ruleId = Objects.requireNonNull(ruleId, "ruleId");
        scopeType = Objects.requireNonNull(scopeType, "scopeType");
        scopeValue = normalizeScopeValue(scopeValue);
        algorithmType = Objects.requireNonNull(algorithmType, "algorithmType");
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be > 0");
        }
        if (burstCapacity <= 0) {
            throw new IllegalArgumentException("burstCapacity must be > 0");
        }
        window = Objects.requireNonNull(window, "window");
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        enforcementMode = Objects.requireNonNull(enforcementMode, "enforcementMode");
        appliesToTiers = normalizeTiers(appliesToTiers);
        updatedAt = Objects.requireNonNullElseGet(updatedAt, Instant::now);
    }

    public boolean matches(RuleContext context) {
        if (!enabled || !appliesToTiers.contains(context.userTier())) {
            return false;
        }

        String candidate = switch (scopeType) {
            case TENANT -> context.tenantId();
            case USER -> context.userId();
            case API -> context.apiPath();
        };
        return scopeValue.equals(WILDCARD) || scopeValue.equals(candidate);
    }

    public String stateKey(RuleContext context) {
        String scopedValue = switch (scopeType) {
            case TENANT -> context.tenantId();
            case USER -> context.userId();
            case API -> context.apiPath();
        };
        return ruleId + ':' + scopedValue;
    }

    public double refillTokensPerSecond() {
        return limit / (window.toNanos() / 1_000_000_000d);
    }

    private static String normalizeScopeValue(String scopeValue) {
        if (scopeValue == null || scopeValue.isBlank()) {
            return WILDCARD;
        }
        return scopeValue.trim();
    }

    private static Set<UserTier> normalizeTiers(Set<UserTier> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            return Collections.unmodifiableSet(EnumSet.allOf(UserTier.class));
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(tiers));
    }
}
