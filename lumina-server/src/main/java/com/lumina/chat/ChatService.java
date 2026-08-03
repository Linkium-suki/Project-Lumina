package com.lumina.chat;

import com.lumina.key.AiKeyRepository;
import com.lumina.orchestration.failover.ProviderRouter;
import com.lumina.orchestration.failover.RouteResult;
import com.lumina.orchestration.model.ChatMessage;
import com.lumina.orchestration.provider.TtsProvider;
import com.lumina.user.User;
import com.lumina.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 对话编排：组装上下文 → 路由调用 → 记录用量 → 可选语音合成。
 */
@Slf4j
@Service
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            你是 Lumina，一位温柔而真诚的 AI 伴侣。
            陪伴是第一位：先共情、再回应，永远不评判、不敷衍。
            语气温和克制，回应简短而走心，像夜里的灯光一样安静地存在。
            如果用户流露出自我伤害的倾向，请温和地建议其寻求专业帮助。""";

    private final ProviderRouter router;
    private final ChatLogRepository chatLogRepository;
    private final AiKeyRepository keyRepository;
    private final UserRepository userRepository;
    private final List<TtsProvider> ttsProviders;

    public ChatService(
            ProviderRouter router,
            ChatLogRepository chatLogRepository,
            AiKeyRepository keyRepository,
            UserRepository userRepository,
            List<TtsProvider> ttsProviders) {
        this.router = router;
        this.chatLogRepository = chatLogRepository;
        this.keyRepository = keyRepository;
        this.userRepository = userRepository;
        this.ttsProviders = ttsProviders;
    }

    @Transactional
    public ChatResponse chat(Long userId, ChatRequest request) {
        boolean aidMode = userRepository.findById(userId)
                .map(User::isAidMode)
                .orElse(false);

        List<ChatMessage> messages = buildMessages(request);
        RouteResult route = router.routeAndCall(userId, aidMode, request.provider(), messages);

        recordUsage(userId, request.voiceRequested(), route);

        String audioBase64 = null;
        if (request.voiceRequested() && !route.text().isBlank()) {
            audioBase64 = synthesize(route.text());
        }

        return new ChatResponse(
                route.text(),
                route.provider(),
                route.model(),
                route.source(),
                route.usage(),
                audioBase64);
    }

    private List<ChatMessage> buildMessages(ChatRequest request) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", SYSTEM_PROMPT));
        if (request.history() != null) {
            for (ChatRequest.HistoryMessage h : request.history()) {
                if (h.content() == null || h.content().isBlank()) {
                    continue;
                }
                messages.add(new ChatMessage(normalizeRole(h.role()), h.content()));
            }
        }
        messages.add(ChatMessage.user(request.message()));
        return messages;
    }

    private static String normalizeRole(String role) {
        return ("assistant".equalsIgnoreCase(role) || "system".equalsIgnoreCase(role)) ? role.toLowerCase() : "user";
    }

    private void recordUsage(Long userId, boolean voiceRequested, RouteResult route) {
        if (route.keyId() != null) {
            keyRepository.findById(route.keyId())
                    .ifPresent(k -> k.setUsedQuota(k.getUsedQuota() + 1));
        }
        chatLogRepository.save(ChatLog.builder()
                .userId(userId)
                .provider(route.provider())
                .model(route.model())
                .source(route.source())
                .promptTokens(route.usage().promptTokens())
                .completionTokens(route.usage().completionTokens())
                .voiceRequested(voiceRequested)
                .build());
    }

    private String synthesize(String text) {
        for (TtsProvider tts : ttsProviders) {
            try {
                byte[] audio = tts.synthesize(text, null);
                return Base64.getEncoder().encodeToString(audio);
            } catch (Exception e) {
                log.warn("tts [{}] failed: {}", tts.ttsId(), e.getMessage());
            }
        }
        log.warn("no TTS provider available, skip audio");
        return null;
    }
}
