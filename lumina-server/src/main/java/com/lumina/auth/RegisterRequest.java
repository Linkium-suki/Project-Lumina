package com.lumina.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "deviceId 不能为空")
        @Size(max = 128, message = "deviceId 过长")
        String deviceId,

        @Size(max = 64, message = "昵称过长")
        String nickname
) {
}
