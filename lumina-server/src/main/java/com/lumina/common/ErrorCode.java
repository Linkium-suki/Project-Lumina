package com.lumina.common;

import lombok.Getter;

/**
 * 统一错误码。code 为对外稳定的机器码，message 为人类可读描述。
 */
@Getter
public enum ErrorCode {

    BAD_REQUEST("BAD_REQUEST", "请求参数错误"),
    UNAUTHORIZED("UNAUTHORIZED", "未认证或凭证已失效"),
    RATE_LIMITED("RATE_LIMITED", "请求过于频繁，请稍后再试"),
    NOT_FOUND("NOT_FOUND", "资源不存在"),
    CONFLICT("CONFLICT", "资源状态冲突"),

    KEY_INVALID("KEY_INVALID", "API Key 校验失败"),
    KEY_NOT_FOUND("KEY_NOT_FOUND", "API Key 不存在"),
    KEY_NOT_OWNER("KEY_NOT_OWNER", "无权操作该 Key"),

    POOL_UNAVAILABLE("POOL_UNAVAILABLE", "互助池当前无可用算力，请稍后再试或使用自有 Key"),
    POOL_LIMIT_EXCEEDED("POOL_LIMIT_EXCEEDED", "今日互助额度已用尽"),
    AID_MODE_REQUIRED("AID_MODE_REQUIRED", "请先开启互助模式"),

    PROVIDER_UNAVAILABLE("PROVIDER_UNAVAILABLE", "所有 AI 提供商均不可用，请稍后再试"),
    PROVIDER_RESPONSE("PROVIDER_RESPONSE", "AI 服务返回异常"),
    TTS_UNAVAILABLE("TTS_UNAVAILABLE", "语音服务不可用"),

    INTERNAL_ERROR("INTERNAL_ERROR", "服务器内部错误");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
