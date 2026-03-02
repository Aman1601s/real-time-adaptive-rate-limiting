package com.platform.ratelimiter.strategy;

import com.platform.ratelimiter.domain.AlgorithmType;
import com.platform.ratelimiter.domain.Decision;
import com.platform.ratelimiter.domain.EnforcementMode;
import com.platform.ratelimiter.domain.RateLimitRule;
import com.platform.ratelimiter.domain.RuleContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class SlidingWindowRateLimitStrategy implements RateLimitStrategy {

    private final ConcurrentMap<String, WindowState> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public SlidingWindowRateLimitStrategy() {
        this(Clock.systemUTC());
    }

    public SlidingWindowRateLimitStrategy(Clock clock) {
        this.clock = clock;
    }

    @Override
    public AlgorithmType supports() {
        return AlgorithmType.SLIDING_WINDOW;
    }

    @Override
    public Decision evaluate(RateLimitRule rule, RuleContext context) {
        String stateKey = rule.stateKey(context);
        WindowState state = windows.computeIfAbsent(stateKey, key -> new WindowState());
        long nowNanos = Instant.now(clock).toEpochMilli() * 1_000_000L;
        long windowNanos = rule.window().toNanos();

        synchronized (state) {
            evictExpired(state.requests, nowNanos, windowNanos);

            if (state.requests.size() < rule.limit()) {
                state.requests.addLast(nowNanos);
                return new Decision.Allowed("sliding-window-allow:" + rule.ruleId());
            }

            long oldest = state.requests.peekFirst() == null ? nowNanos : state.requests.peekFirst();
            long retryAfterNanos = Math.max(1L, windowNanos - (nowNanos - oldest));
            Duration retryAfter = Duration.ofNanos(retryAfterNanos);

            if (rule.enforcementMode() == EnforcementMode.SOFT_THROTTLE) {
                return new Decision.Throttled("sliding-window-soft-throttle:" + rule.ruleId(), retryAfter, true);
            }

            return new Decision.Rejected("sliding-window-hard-reject:" + rule.ruleId(), retryAfter);
        }
    }

    private static void evictExpired(Deque<Long> requests, long nowNanos, long windowNanos) {
        while (!requests.isEmpty()) {
            Long oldest = requests.peekFirst();
            if (oldest == null || nowNanos - oldest < windowNanos) {
                return;
            }
            requests.pollFirst();
        }
    }

    private static final class WindowState {
        private final Deque<Long> requests = new ArrayDeque<>();
    }
}
