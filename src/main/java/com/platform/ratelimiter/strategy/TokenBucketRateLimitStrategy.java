package com.platform.ratelimiter.strategy;

import com.platform.ratelimiter.domain.AlgorithmType;
import com.platform.ratelimiter.domain.Decision;
import com.platform.ratelimiter.domain.EnforcementMode;
import com.platform.ratelimiter.domain.RateLimitRule;
import com.platform.ratelimiter.domain.RuleContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class TokenBucketRateLimitStrategy implements RateLimitStrategy {

    private final ConcurrentMap<String, BucketState> buckets = new ConcurrentHashMap<>();
    private final Clock clock;

    public TokenBucketRateLimitStrategy() {
        this(Clock.systemUTC());
    }

    public TokenBucketRateLimitStrategy(Clock clock) {
        this.clock = clock;
    }

    @Override
    public AlgorithmType supports() {
        return AlgorithmType.TOKEN_BUCKET;
    }

    @Override
    public Decision evaluate(RateLimitRule rule, RuleContext context) {
        String stateKey = rule.stateKey(context);
        BucketState state = buckets.computeIfAbsent(
                stateKey,
                key -> new BucketState(Math.max(rule.burstCapacity(), rule.limit()), Instant.now(clock))
        );

        Instant now = Instant.now(clock);

        synchronized (state) {
            refill(state, rule, now);

            if (state.tokens >= 1.0d) {
                state.tokens -= 1.0d;
                return new Decision.Allowed("token-bucket-allow:" + rule.ruleId());
            }

            Duration retryAfter = computeRetryAfter(rule, state.tokens);
            if (rule.enforcementMode() == EnforcementMode.SOFT_THROTTLE) {
                return new Decision.Throttled("token-bucket-soft-throttle:" + rule.ruleId(), retryAfter, true);
            }

            return new Decision.Rejected("token-bucket-hard-reject:" + rule.ruleId(), retryAfter);
        }
    }

    private static void refill(BucketState state, RateLimitRule rule, Instant now) {
        Duration elapsed = Duration.between(state.lastRefillAt, now);
        if (elapsed.isZero() || elapsed.isNegative()) {
            return;
        }

        double refillRatePerSecond = rule.limit() / (double) rule.window().toNanos() * 1_000_000_000d;
        double refillTokens = elapsed.toNanos() / 1_000_000_000d * refillRatePerSecond;
        double maxCapacity = Math.max(rule.burstCapacity(), rule.limit());

        state.tokens = Math.min(maxCapacity, state.tokens + refillTokens);
        state.lastRefillAt = now;
    }

    private static Duration computeRetryAfter(RateLimitRule rule, double currentTokens) {
        double refillRatePerSecond = rule.limit() / (double) rule.window().toNanos() * 1_000_000_000d;
        if (refillRatePerSecond <= 0d) {
            return rule.window();
        }

        double missingTokens = Math.max(0d, 1d - currentTokens);
        long nanos = (long) Math.ceil((missingTokens / refillRatePerSecond) * 1_000_000_000d);
        return Duration.ofNanos(Math.max(1L, nanos));
    }

    private static final class BucketState {
        private double tokens;
        private Instant lastRefillAt;

        private BucketState(double tokens, Instant lastRefillAt) {
            this.tokens = tokens;
            this.lastRefillAt = lastRefillAt;
        }
    }
}
