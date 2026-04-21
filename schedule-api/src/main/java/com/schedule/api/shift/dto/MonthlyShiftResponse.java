package com.schedule.api.shift.dto;

import java.util.List;

public record MonthlyShiftResponse(
        int year,
        int month,
        int replacedCount,
        List<ShiftResponse> items
) {
}
