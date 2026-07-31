package com.lumina.common.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内固定窗口限流器。适用于单实例部署；
 * 多实例时请替换为 Redis 等分布式实现。
 */
@Slf4j
@Component
public class RateLimiter {

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int requestsPerMinute;

    public RateLimiter(@Value("${lumina.rate-limit.requests-per-minute:30}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public boolean tryAcquire(String key) {
        long now = Instant.now().getEpochSecond();
        long bucket = now / 60;
        Window w = windows.compute(key, (k, old) -> {
            if (old == null || old.bucket != bucket) {
                return new Window(bucket, 1);
            }
            old.count++;
            return old;
        });
        return w.count <= requestsPerMinute;
    }

    private static final class Window {
        private final long bucket;
        private int count;

        private Window(long bucket, int count) {
            this.bucket = bucket;
            this.count = count;
        }
    }
}
