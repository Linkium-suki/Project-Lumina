package com.lumina.key;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KeyRequest(
        @NotNull(message = "provider 不能为空")
        AiProvider provider,

        @Size(max = 64, message = "model 过长")
        String model,

        @NotBlank(message = "apiKey 不能为空")
        @Size(max = 512, message = "apiKey 过长")
        String apiKey
) {
}
