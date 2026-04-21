package com.schedule.api.shift.dto;

import java.time.LocalDate;

public record ShiftDateResponse(
        LocalDate date,
        ShiftResponse item
) {
}
