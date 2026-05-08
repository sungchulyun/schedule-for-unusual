package com.schedule.api.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FcmTokenRequest(
        @NotBlank(message = "FCM 토큰을 입력해야 합니다.")
        @Size(max = 512, message = "FCM 토큰은 512자 이하여야 합니다.")
        String token,

        @Size(max = 30, message = "플랫폼은 30자 이하여야 합니다.")
        String platform
) {
}
