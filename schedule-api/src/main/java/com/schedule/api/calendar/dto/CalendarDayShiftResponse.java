package com.schedule.api.calendar.dto;

import com.schedule.api.shift.domain.ShiftType;

public record CalendarDayShiftResponse(
        String id,
        String ownerUserId,
        String ownerType,
        ShiftType shiftType
) {
}
