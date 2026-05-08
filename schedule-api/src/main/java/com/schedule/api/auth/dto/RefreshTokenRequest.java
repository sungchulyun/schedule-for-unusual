package com.schedule.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "리프레시 토큰을 입력해야 합니다.") String refreshToken
) {
}
