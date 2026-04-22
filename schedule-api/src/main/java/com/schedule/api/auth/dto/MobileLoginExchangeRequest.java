package com.schedule.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record MobileLoginExchangeRequest(
        @NotBlank String loginCode
) {
}
