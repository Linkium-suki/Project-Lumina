package com.lumina.orchestration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 守夜人兜底配置：互助池枯竭时使用作者自持 Key。
 */
@ConfigurationProperties(prefix = "lumina.watchman")
public record WatchmanProperties(
        boolean enabled,
        List<KeyConfig> keys) {

    public record KeyConfig(String provider, String apiKey, String model) {
    }
}
