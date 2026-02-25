package com.platform.ratelimiter.domain;

import java.time.Duration;
import java.util.Objects;

public sealed interface Decision permits Decision.Allowed, Decision.Throttled, Decision.Rejected {

    enum DecisionType {
        ALLOWED,
        THROTTLED,
        REJECTED
    }

    DecisionType type();

    String reason();

    default Duration retryAfter() {
        return Duration.ZERO;
    }

    record Allowed(String reason) implements Decision {
        public Allowed {
            Objects.requireNonNull(reason, "reason");
        }

        @Override
        public DecisionType type() {
            return DecisionType.ALLOWED;
        }
    }

    record Throttled(String reason, Duration retryAfter, boolean soft) implements Decision {
        public Throttled {
            Objects.requireNonNull(reason, "reason");
            retryAfter = retryAfter == null || retryAfter.isNegative() ? Duration.ZERO : retryAfter;
        }

        @Override
        public DecisionType type() {
            return DecisionType.THROTTLED;
        }
    }

    record Rejected(String reason, Duration retryAfter) implements Decision {
        public Rejected {
            Objects.requireNonNull(reason, "reason");
            retryAfter = retryAfter == null || retryAfter.isNegative() ? Duration.ZERO : retryAfter;
        }

        @Override
        public DecisionType type() {
            return DecisionType.REJECTED;
        }
    }
}
