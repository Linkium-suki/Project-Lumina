package com.lumina.orchestration.provider;

/**
 * 语音合成（TTS）适配器。
 */
public interface TtsProvider {

    String ttsId();

    /**
     * 合成语音字节（MP3 等音频格式）。
     */
    byte[] synthesize(String text, String voice);
}
