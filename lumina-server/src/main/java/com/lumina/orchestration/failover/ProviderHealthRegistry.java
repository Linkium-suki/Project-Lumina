package com.lumina.orchestration.failover;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内健康登记：连续失败后进入冷却期，Router 优先跳过，
 * 避免对已故障的提供商发起无谓的超时请求。
 */
@Component
public class ProviderHealthRegistry {

    private static final int CONSECUTIVE_FAILURE_THRESHOLD = 2;
    private static final Duration COOLDOWN = Duration.ofSeconds(30);

    private final ConcurrentHashMap<String, ProviderState> states = new ConcurrentHashMap<>();

    public boolean isHealthy(String providerId) {
        ProviderState state = states.get(providerId);
        return state == null
                || state.consecutiveFailures < CONSECUTIVE_FAILURE_THRESHOLD
                || state.lastFailure.plus(COOLDOWN).isBefore(Instant.now());
    }

    public void recordSuccess(String providerId) {
        states.remove(providerId);
    }

    public void recordFailure(String providerId) {
        states.compute(providerId, (k, state) -> {
            if (state == null) {
                return new ProviderState(1, Instant.now());
            }
            return new ProviderState(state.consecutiveFailures + 1, Instant.now());
        });
    }

    private record ProviderState(int consecutiveFailures, Instant lastFailure) {
    }
}
