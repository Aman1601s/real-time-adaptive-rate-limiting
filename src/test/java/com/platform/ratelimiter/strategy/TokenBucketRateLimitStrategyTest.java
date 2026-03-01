package com.platform.ratelimiter.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import com.platform.ratelimiter.domain.AlgorithmType;
import com.platform.ratelimiter.domain.Decision;
import com.platform.ratelimiter.domain.EnforcementMode;
import com.platform.ratelimiter.domain.RateLimitRule;
import com.platform.ratelimiter.domain.RuleContext;
import com.platform.ratelimiter.domain.ScopeType;
import com.platform.ratelimiter.domain.UserTier;
import com.platform.ratelimiter.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TokenBucketRateLimitStrategyTest {

    @Test
    void shouldAllowWithinBurstAndRefillAfterWindowProgresses() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        TokenBucketRateLimitStrategy strategy = new TokenBucketRateLimitStrategy(clock);

        RateLimitRule rule = new RateLimitRule(
                "tb-rule",
                ScopeType.USER,
                "user-1",
                AlgorithmType.TOKEN_BUCKET,
                2,
                2,
                Duration.ofSeconds(1),
                EnforcementMode.HARD_REJECT,
                Set.of(UserTier.FREE),
                100,
                true,
                clock.instant());

        RuleContext context = new RuleContext("tenant-1", "user-1", "/api/v1/data/1", UserTier.FREE, "corr-1", clock.instant());

        assertThat(strategy.evaluate(rule, context)).isInstanceOf(Decision.Allowed.class);
        assertThat(strategy.evaluate(rule, context)).isInstanceOf(Decision.Allowed.class);
        assertThat(strategy.evaluate(rule, context)).isInstanceOf(Decision.Rejected.class);

        clock.advance(Duration.ofMillis(500));
        assertThat(strategy.evaluate(rule, context)).isInstanceOf(Decision.Allowed.class);
    }
}
