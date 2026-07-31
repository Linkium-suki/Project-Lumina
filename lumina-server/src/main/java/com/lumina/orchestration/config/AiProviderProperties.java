package com.lumina.orchestration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * 映射 application.yml 中 {@code lumina-ai.*} 配置。
 * 列表顺序即故障转移优先级。
 */
@ConfigurationProperties(prefix = "lumina-ai")
public record AiProviderProperties(
        List<ChatConfig> chat,
        List<TtsConfig> tts) {

    public record ChatConfig(
            String id,
            String baseUrl,
            @DefaultValue({"default"}) List<String> models) {
    }

    public record TtsConfig(
            String id,
            String region,
            String apiKey,
            @DefaultValue("zh-CN-XiaoxiaoNeural") String voice) {
    }
}
