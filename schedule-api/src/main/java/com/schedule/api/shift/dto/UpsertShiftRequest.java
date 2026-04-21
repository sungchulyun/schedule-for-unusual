package com.schedule.api.shift.dto;

import com.schedule.api.shift.domain.ShiftType;
import jakarta.validation.constraints.NotNull;

public record UpsertShiftRequest(
        @NotNull ShiftType shiftType
) {
}
