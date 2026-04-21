package com.schedule.api.calendar.dto;

import com.schedule.api.event.domain.EventOwnerType;
import com.schedule.api.event.domain.EventSubjectType;
import java.time.LocalDate;

public record CalendarDayEventResponse(
        String id,
        String title,
        EventSubjectType subjectType,
        String ownerUserId,
        EventOwnerType ownerType,
        LocalDate startDate,
        LocalDate endDate,
        boolean isMultiDay
) {
}
