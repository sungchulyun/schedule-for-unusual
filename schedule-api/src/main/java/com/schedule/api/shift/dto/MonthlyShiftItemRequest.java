package com.schedule.api.shift.dto;

import com.schedule.api.shift.domain.ShiftType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MonthlyShiftItemRequest(
        @NotNull(message = "근무 날짜를 입력해야 합니다.") LocalDate date,
        @NotNull(message = "근무 유형을 입력해야 합니다.") ShiftType shiftType
) {
}
