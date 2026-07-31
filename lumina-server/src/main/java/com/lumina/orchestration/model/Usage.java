package com.lumina.orchestration.model;

/**
 * Token 用量。
 */
public record Usage(int promptTokens, int completionTokens) {

    public static final Usage EMPTY = new Usage(0, 0);
}
