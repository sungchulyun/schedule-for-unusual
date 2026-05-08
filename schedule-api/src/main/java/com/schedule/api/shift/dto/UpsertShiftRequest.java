package com.schedule.api.shift.dto;

import com.schedule.api.shift.domain.ShiftType;
import jakarta.validation.constraints.NotNull;

public record UpsertShiftRequest(
        @NotNull(message = "근무 유형을 입력해야 합니다.") ShiftType shiftType
) {
}
