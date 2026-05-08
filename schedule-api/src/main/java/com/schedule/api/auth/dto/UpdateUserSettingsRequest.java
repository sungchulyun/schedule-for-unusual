package com.schedule.api.auth.dto;

import com.schedule.api.auth.domain.DefaultShiftOwnerType;
import jakarta.validation.constraints.NotNull;

public record UpdateUserSettingsRequest(
        @NotNull(message = "기본 근무 조회 대상을 입력해야 합니다.") DefaultShiftOwnerType defaultShiftOwnerType
) {
}
