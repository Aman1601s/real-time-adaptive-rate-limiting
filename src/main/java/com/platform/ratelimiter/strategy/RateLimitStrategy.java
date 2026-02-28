package com.platform.ratelimiter.strategy;

import com.platform.ratelimiter.domain.AlgorithmType;
import com.platform.ratelimiter.domain.Decision;
import com.platform.ratelimiter.domain.RateLimitRule;
import com.platform.ratelimiter.domain.RuleContext;

public interface RateLimitStrategy {

    AlgorithmType supports();

    Decision evaluate(RateLimitRule rule, RuleContext context);
}
