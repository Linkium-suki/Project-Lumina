package com.lumina.chat;

import com.lumina.orchestration.failover.RouteSource;
import com.lumina.orchestration.model.Usage;

public record ChatResponse(
        String text,
        String provider,
        String model,
        RouteSource source,
        Usage usage,
        String audioBase64
) {
}
