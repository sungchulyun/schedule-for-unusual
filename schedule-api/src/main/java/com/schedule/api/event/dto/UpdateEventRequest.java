package com.schedule.api.event.dto;

import com.schedule.api.event.domain.EventSubjectType;
import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateEventRequest(
        String title,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        EventSubjectType subjectType,
        String ownerUserId,
        String note
) {
}
