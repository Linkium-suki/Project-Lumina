package com.lumina.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 对话请求。history 为可选的多轮上下文（不含本消息）。
 */
public record ChatRequest(
        @NotBlank(message = "message 不能为空")
        @Size(max = 4000, message = "消息过长")
        String message,

        List<HistoryMessage> history,

        Boolean voice,

        @Size(max = 32, message = "provider 过长")
        String provider
) {
    public record HistoryMessage(String role, String content) {
    }

    public boolean voiceRequested() {
        return Boolean.TRUE.equals(voice);
    }
}
