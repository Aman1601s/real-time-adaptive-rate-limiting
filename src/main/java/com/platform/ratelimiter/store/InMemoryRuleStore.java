package com.platform.ratelimiter.store;

import com.platform.ratelimiter.domain.RateLimitRule;
import com.platform.ratelimiter.domain.RuleContext;
import com.platform.ratelimiter.domain.ScopeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRuleStore implements RuleStore {

    private static final String WILDCARD = "*";

    private final ConcurrentMap<String, RateLimitRule> rulesById = new ConcurrentHashMap<>();
    private final EnumMap<ScopeType, ConcurrentMap<String, Set<String>>> scopeIndex = new EnumMap<>(ScopeType.class);

    public InMemoryRuleStore() {
        for (ScopeType scope : ScopeType.values()) {
            scopeIndex.put(scope, new ConcurrentHashMap<>());
        }
    }

    @Override
    public void upsert(RateLimitRule rule) {
        rulesById.compute(rule.ruleId(), (id, existing) -> {
            if (existing != null) {
                removeIndex(existing);
            }
            addIndex(rule);
            return rule;
        });
    }

    @Override
    public void delete(String ruleId) {
        RateLimitRule removed = rulesById.remove(ruleId);
        if (removed != null) {
            removeIndex(removed);
        }
    }

    @Override
    public List<RateLimitRule> findMatchingRules(RuleContext context) {
        Set<String> candidateIds = new HashSet<>();

        for (ScopeType scope : ScopeType.values()) {
            String value = scopeValue(scope, context);
            ConcurrentMap<String, Set<String>> index = scopeIndex.get(scope);
            addIfPresent(index, value, candidateIds);
            addIfPresent(index, WILDCARD, candidateIds);
        }

        if (candidateIds.isEmpty()) {
            return List.of();
        }

        List<RateLimitRule> matches = new ArrayList<>(candidateIds.size());
        for (String id : candidateIds) {
            RateLimitRule rule = rulesById.get(id);
            if (rule != null && rule.matches(context)) {
                matches.add(rule);
            }
        }

        matches.sort(Comparator.comparingInt(RateLimitRule::priority).reversed());
        return matches;
    }

    @Override
    public Collection<RateLimitRule> getAll() {
        return Collections.unmodifiableCollection(new ArrayList<>(rulesById.values()));
    }

    @Override
    public Optional<RateLimitRule> getById(String ruleId) {
        return Optional.ofNullable(rulesById.get(ruleId));
    }

    private void addIndex(RateLimitRule rule) {
        scopeIndex
                .get(rule.scopeType())
                .computeIfAbsent(rule.scopeValue(), key -> ConcurrentHashMap.newKeySet())
                .add(rule.ruleId());
    }

    private void removeIndex(RateLimitRule rule) {
        Set<String> ruleIds = scopeIndex
                .get(rule.scopeType())
                .getOrDefault(rule.scopeValue(), Collections.emptySet());

        if (!ruleIds.isEmpty()) {
            ruleIds.remove(rule.ruleId());
            if (ruleIds.isEmpty()) {
                scopeIndex.get(rule.scopeType()).remove(rule.scopeValue());
            }
        }
    }

    private static void addIfPresent(ConcurrentMap<String, Set<String>> index, String key, Set<String> candidateIds) {
        Set<String> set = index.get(key);
        if (set != null) {
            candidateIds.addAll(set);
        }
    }

    private static String scopeValue(ScopeType scopeType, RuleContext context) {
        return switch (scopeType) {
            case TENANT -> context.tenantId();
            case USER -> context.userId();
            case API -> context.apiPath();
        };
    }
}
