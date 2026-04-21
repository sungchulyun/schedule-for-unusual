package com.schedule.api.calendar.dto;

import com.schedule.api.event.dto.EventResponse;
import com.schedule.api.shift.dto.ShiftResponse;
import java.util.List;

public record CalendarMonthResponse(
        int year,
        int month,
        CalendarFilterResponse filters,
        CalendarMetaResponse meta,
        List<EventResponse> events,
        List<ShiftResponse> shifts,
        List<CalendarDaySummaryResponse> days
) {
}
