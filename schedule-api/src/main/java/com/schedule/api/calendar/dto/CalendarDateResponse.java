package com.schedule.api.calendar.dto;

import com.schedule.api.event.dto.EventResponse;
import com.schedule.api.shift.dto.ShiftResponse;
import java.time.LocalDate;
import java.util.List;

public record CalendarDateResponse(
        LocalDate date,
        CalendarMetaResponse meta,
        ShiftResponse shift,
        List<EventResponse> events
) {
}
