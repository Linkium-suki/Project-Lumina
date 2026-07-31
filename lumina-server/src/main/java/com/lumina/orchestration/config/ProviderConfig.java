package com.lumina.orchestration.config;

import com.lumina.orchestration.provider.AzureTtsProvider;
import com.lumina.orchestration.provider.ChatProvider;
import com.lumina.orchestration.provider.OpenAiCompatProvider;
import com.lumina.orchestration.provider.TtsProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 依据配置构建所有 Provider 适配器。
 */
@Configuration
@EnableConfigurationProperties(AiProviderProperties.class)
public class ProviderConfig {

    @Bean
    public List<ChatProvider> chatProviders(AiProviderProperties props) {
        if (props.chat() == null) {
            return List.of();
        }
        return props.chat().stream()
                .map(c -> new OpenAiCompatProvider(c.id(), c.baseUrl(), c.models()))
                .map(ChatProvider.class::cast)
                .toList();
    }

    @Bean
    public List<TtsProvider> ttsProviders(AiProviderProperties props) {
        if (props.tts() == null) {
            return List.of();
        }
        return props.tts().stream()
                .map(t -> new AzureTtsProvider(t.region(), t.apiKey(), t.voice()))
                .map(TtsProvider.class::cast)
                .toList();
    }
}
