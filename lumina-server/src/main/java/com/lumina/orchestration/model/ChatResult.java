package com.lumina.orchestration.model;

/**
 * 单次对话结果。
 */
public record ChatResult(String text, String provider, String model, Usage usage) {
}
