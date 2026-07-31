package com.lumina.pool;

public record PoolStatusResponse(
        boolean aidMode,
        int dailyLimit,
        long todayUsage,
        long availablePoolKeys
) {
}
