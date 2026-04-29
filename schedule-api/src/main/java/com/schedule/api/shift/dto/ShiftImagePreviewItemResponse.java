package com.schedule.api.shift.dto;

import com.schedule.api.shift.domain.ShiftType;
import java.time.LocalDate;

public record ShiftImagePreviewItemResponse(
        LocalDate date,
        int day,
        String rawCode,
        String normalizedCode,
        ShiftType shiftType,
        boolean editable
) {
}
