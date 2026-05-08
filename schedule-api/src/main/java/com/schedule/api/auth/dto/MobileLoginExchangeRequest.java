package com.schedule.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record MobileLoginExchangeRequest(
        @NotBlank(message = "로그인 코드를 입력해야 합니다.") String loginCode
) {
}
