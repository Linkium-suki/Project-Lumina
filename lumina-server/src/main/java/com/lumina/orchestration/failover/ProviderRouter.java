package com.lumina.orchestration.failover;

import com.lumina.common.ErrorCode;
import com.lumina.common.exception.BizException;
import com.lumina.key.AiKey;
import com.lumina.key.AiKeyRepository;
import com.lumina.key.AiProvider;
import com.lumina.key.KeyEncryptor;
import com.lumina.key.KeyStatus;
import com.lumina.orchestration.config.WatchmanProperties;
import com.lumina.orchestration.model.ChatMessage;
import com.lumina.orchestration.model.ChatResult;
import com.lumina.orchestration.provider.ChatProvider;
import com.lumina.orchestration.provider.ProviderException;
import com.lumina.pool.PoolService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 故障转移调度核心。
 *
 * <p>路由优先级：
 * <ol>
 *   <li>用户自有 Key（按提供商顺序）</li>
 *   <li>互助池捐赠 Key（仅互助模式）</li>
 *   <li>守夜人兜底 Key（池枯竭时）</li>
 * </ol>
 *
 * <p>同级内按提供商配置顺序尝试；每个提供商经 CircuitBreaker + Retry 保护，
 * 失败即登记健康记录并降级到下一个候选。
 */
@Slf4j
@Component
@EnableConfigurationProperties(WatchmanProperties.class)
public class ProviderRouter {

    private final List<ChatProvider> chatProviders;
    private final AiKeyRepository keyRepository;
    private final KeyEncryptor keyEncryptor;
    private final PoolService poolService;
    private final WatchmanProperties watchmanProperties;
    private final CircuitBreakerRegistry breakerRegistry;
    private final RetryRegistry retryRegistry;
    private final ProviderHealthRegistry healthRegistry;

    public ProviderRouter(
            List<ChatProvider> chatProviders,
            AiKeyRepository keyRepository,
            KeyEncryptor keyEncryptor,
            PoolService poolService,
            WatchmanProperties watchmanProperties,
            CircuitBreakerRegistry breakerRegistry,
            RetryRegistry retryRegistry,
            ProviderHealthRegistry healthRegistry) {
        this.chatProviders = chatProviders;
        this.keyRepository = keyRepository;
        this.keyEncryptor = keyEncryptor;
        this.poolService = poolService;
        this.watchmanProperties = watchmanProperties;
        this.breakerRegistry = breakerRegistry;
        this.retryRegistry = retryRegistry;
        this.healthRegistry = healthRegistry;
    }

    /**
     * 路由并执行一次对话。
     *
     * @param userId           当前用户
     * @param aidMode          是否处于互助模式
     * @param preferredProvider 可选：仅使用指定提供商（null 表示全部按序尝试）
     */
    public RouteResult routeAndCall(
            Long userId, boolean aidMode, String preferredProvider, List<ChatMessage> messages) {

        List<ChatProvider> providers = filter(preferredProvider);
        if (providers.isEmpty()) {
            throw new BizException(ErrorCode.PROVIDER_UNAVAILABLE, "未配置任何 AI 提供商");
        }

        List<String> failures = new ArrayList<>();

        for (ChatProvider provider : providers) {
            AiProvider aiProvider = toEnum(provider.providerId());
            if (aiProvider == null) {
                continue;
            }

            // 1) 用户自有 Key
            Optional<AiKey> own = keyRepository
                    .findByOwnerUserIdAndProviderAndStatus(userId, aiProvider, KeyStatus.PRIVATE)
                    .stream().findFirst();

            // 2) 互助池 Key（互助模式 + 池内有货）
            AiKey poolKey = null;
            if (own.isEmpty() && aidMode) {
                poolKey = poolService.acquireKey(userId, aiProvider);
            }

            // 3) 守夜人兜底
            WatchmanProperties.KeyConfig watchman = findWatchmanKey(provider.providerId());

            try {
                if (own.isPresent()) {
                    return call(provider, own.get(), RouteSource.OWN, messages);
                }
                if (poolKey != null) {
                    try {
                        return call(provider, poolKey, RouteSource.POOL, messages);
                    } finally {
                        poolService.releaseKey(poolKey.getId());
                    }
                }
                if (watchman != null) {
                    return call(provider, watchman.apiKey(), watchman.model(),
                            RouteSource.WATCHMAN, messages, null);
                }
            } catch (ProviderException e) {
                healthRegistry.recordFailure(provider.providerId());
                failures.add(provider.providerId() + ": " + e.getMessage());
                log.warn("provider [{}] failed, failover to next: {}", provider.providerId(), e.getMessage());
            }
        }

        throw new BizException(ErrorCode.PROVIDER_UNAVAILABLE,
                "所有 AI 提供商均不可用。失败明细: " + String.join(" | ", failures));
    }

    private RouteResult call(ChatProvider provider, AiKey key, RouteSource source,
                             List<ChatMessage> messages) {
        String model = key.getModel() != null
                ? key.getModel()
                : provider.supportedModels().getFirst();
        return call(provider, keyEncryptor.decrypt(key.getEncryptedKey()), model, source, messages, key.getId());
    }

    private RouteResult call(ChatProvider provider, String apiKey, String model, RouteSource source,
                             List<ChatMessage> messages, Long keyId) {
        Supplier<ChatResult> action = () -> provider.chat(messages, model, apiKey);
        CircuitBreaker breaker = breakerRegistry.circuitBreaker(provider.providerId());
        Retry retry = retryRegistry.retry("chat");
        ChatResult result = retry.executeSupplier(() -> breaker.executeSupplier(action));
        healthRegistry.recordSuccess(provider.providerId());
        return new RouteResult(
                result.text(), result.provider(), result.model(), source, keyId, result.usage());
    }

    private List<ChatProvider> filter(String preferredProvider) {
        if (preferredProvider == null || preferredProvider.isBlank()) {
            return chatProviders;
        }
        return chatProviders.stream()
                .filter(p -> p.providerId().equalsIgnoreCase(preferredProvider))
                .toList();
    }

    private AiProvider toEnum(String providerId) {
        try {
            return AiProvider.valueOf(providerId.toLowerCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private WatchmanProperties.KeyConfig findWatchmanKey(String providerId) {
        if (!watchmanProperties.enabled() || watchmanProperties.keys() == null) {
            return null;
        }
        return watchmanProperties.keys().stream()
                .filter(k -> k.provider().equalsIgnoreCase(providerId))
                .filter(k -> k.apiKey() != null && !k.apiKey().isBlank())
                .findFirst()
                .orElse(null);
    }
}
