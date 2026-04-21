package com.schedule.api.shift.dto;

import com.schedule.api.shift.domain.ShiftType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MonthlyShiftItemRequest(
        @NotNull LocalDate date,
        @NotNull ShiftType shiftType
) {
}
