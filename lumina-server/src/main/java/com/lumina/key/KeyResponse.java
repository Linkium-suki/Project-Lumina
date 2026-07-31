package com.lumina.key;

import java.time.Instant;

/**
 * Key 对外视图：永远不回传明文。
 */
public record KeyResponse(
        Long id,
        AiProvider provider,
        String model,
        KeyStatus status,
        long usedQuota,
        Instant expiresAt
) {
}
