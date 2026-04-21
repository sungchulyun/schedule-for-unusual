package com.schedule.api.calendar.dto;

import java.time.LocalDate;
import java.util.List;

public record CalendarDaySummaryResponse(
        LocalDate date,
        CalendarDayShiftResponse shift,
        List<CalendarDayEventResponse> events
) {
}
