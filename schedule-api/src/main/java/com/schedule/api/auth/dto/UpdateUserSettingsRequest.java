package com.schedule.api.auth.dto;

import com.schedule.api.auth.domain.DefaultShiftOwnerType;
import jakarta.validation.constraints.NotNull;

public record UpdateUserSettingsRequest(
        @NotNull DefaultShiftOwnerType defaultShiftOwnerType
) {
}
