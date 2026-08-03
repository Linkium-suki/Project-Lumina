package com.lumina.orchestration.failover;

import com.lumina.orchestration.model.Usage;

/**
 * 一次路由+调用的结果。
 */
public record RouteResult(
        String text,
        String provider,
        String model,
        RouteSource source,
        Long keyId,
        Usage usage) {
}
