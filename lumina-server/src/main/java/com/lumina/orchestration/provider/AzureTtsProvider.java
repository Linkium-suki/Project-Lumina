package com.lumina.orchestration.provider;

import com.lumina.common.ErrorCode;
import com.lumina.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.net.URI;

/**
 * Azure 文本转语音（REST + SSML）。
 */
@Slf4j
public class AzureTtsProvider implements TtsProvider {

    private static final String OUTPUT_FORMAT = "audio-24khz-48kbitrate-mono-mp3";

    private final String region;
    private final String apiKey;
    private final String defaultVoice;
    private final RestClient restClient;

    public AzureTtsProvider(String region, String apiKey, String defaultVoice) {
        this.region = region;
        this.apiKey = apiKey;
        this.defaultVoice = defaultVoice;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String ttsId() {
        return "azure";
    }

    @Override
    public byte[] synthesize(String text, String voice) {
        try {
            String effectiveVoice = (voice == null || voice.isBlank()) ? defaultVoice : voice;
            String ssml = """
                    <speak version='1.0' xml:lang='zh-CN'>
                      <voice name='%s'>%s</voice>
                    </speak>
                    """.formatted(effectiveVoice, escapeXml(text));

            byte[] audio = restClient.post()
                    .uri(URI.create("https://%s.tts.speech.microsoft.com/cognitiveservices/v1".formatted(region)))
                    .header("Ocp-Apim-Subscription-Key", apiKey)
                    .header("X-Microsoft-OutputFormat", OUTPUT_FORMAT)
                    .contentType(MediaType.APPLICATION_XML)
                    .body(ssml)
                    .retrieve()
                    .onStatus(status -> status.isError(), (req, res) -> {
                        throw new BizException(ErrorCode.TTS_UNAVAILABLE,
                                "TTS HTTP " + res.getStatusCode());
                    })
                    .body(byte[].class);
            return audio == null ? new byte[0] : audio;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.TTS_UNAVAILABLE, "TTS 调用失败: " + e.getMessage());
        }
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
