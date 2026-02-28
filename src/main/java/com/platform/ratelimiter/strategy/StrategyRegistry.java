package com.platform.ratelimiter.strategy;

import com.platform.ratelimiter.domain.AlgorithmType;
import java.util.EnumMap;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StrategyRegistry {

    private final EnumMap<AlgorithmType, RateLimitStrategy> strategies = new EnumMap<>(AlgorithmType.class);

    public StrategyRegistry(List<RateLimitStrategy> strategies) {
        for (RateLimitStrategy strategy : strategies) {
            this.strategies.put(strategy.supports(), strategy);
        }
    }

    public RateLimitStrategy strategyFor(AlgorithmType algorithmType) {
        RateLimitStrategy strategy = strategies.get(algorithmType);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy registered for " + algorithmType);
        }
        return strategy;
    }
}
