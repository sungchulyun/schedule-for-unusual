package com.schedule.api.calendar.dto;

import com.schedule.api.event.domain.EventOwnerType;
import com.schedule.api.event.domain.EventSubjectType;
import java.time.LocalDate;
import java.time.LocalTime;

public record CalendarDayEventResponse(
        String id,
        String title,
        EventSubjectType subjectType,
        String ownerUserId,
        EventOwnerType ownerType,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        boolean isMultiDay
) {
}
