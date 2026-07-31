package com.lumina.orchestration.provider;

import com.lumina.orchestration.model.ChatMessage;
import com.lumina.orchestration.model.ChatResult;

import java.util.List;

/**
 * 大模型对话提供方适配器。实现需保证无状态、线程安全。
 * 各实现均不持有 Key，Key 由调用方按请求传入（来自自有/互助池/守夜人）。
 */
public interface ChatProvider {

    /** 稳定的提供商标识，与 AiProvider 枚举对齐。 */
    String providerId();

    /** 该提供商可用的模型列表（优先级从高到低）。 */
    List<String> supportedModels();

    /** 调用模型，失败抛 ProviderException。 */
    ChatResult chat(List<ChatMessage> messages, String model, String apiKey);
}
