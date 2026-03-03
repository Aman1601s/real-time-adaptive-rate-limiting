package com.platform.ratelimiter.store;

import com.platform.ratelimiter.domain.RateLimitRule;
import com.platform.ratelimiter.domain.RuleContext;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RuleStore {

    void upsert(RateLimitRule rule);

    void delete(String ruleId);

    List<RateLimitRule> findMatchingRules(RuleContext context);

    Collection<RateLimitRule> getAll();

    Optional<RateLimitRule> getById(String ruleId);
}
