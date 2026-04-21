package com.schedule.api.shift.dto;

import java.util.List;

public record ShiftMonthResponse(
        int year,
        int month,
        List<ShiftResponse> items
) {
}
