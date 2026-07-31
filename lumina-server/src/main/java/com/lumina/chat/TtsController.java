package com.lumina.chat;

import com.lumina.common.ApiResponse;
import com.lumina.common.ErrorCode;
import com.lumina.common.exception.BizException;
import com.lumina.orchestration.provider.TtsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tts")
@RequiredArgsConstructor
public class TtsController {

    private final List<TtsProvider> ttsProviders;

    @GetMapping
    public ApiResponse<String> synthesize(@RequestParam String text, @RequestParam(required = false) String voice) {
        for (TtsProvider tts : ttsProviders) {
            try {
                byte[] audio = tts.synthesize(text, voice);
                return ApiResponse.ok(Base64.getEncoder().encodeToString(audio));
            } catch (Exception e) {
                // try next provider
            }
        }
        throw new BizException(ErrorCode.TTS_UNAVAILABLE);
    }
}
