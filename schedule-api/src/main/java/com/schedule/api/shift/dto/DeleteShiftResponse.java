package com.schedule.api.shift.dto;

import java.time.Instant;
import java.time.LocalDate;

public record DeleteShiftResponse(
        LocalDate date,
        boolean deleted,
        Instant deletedAt
) {
}
