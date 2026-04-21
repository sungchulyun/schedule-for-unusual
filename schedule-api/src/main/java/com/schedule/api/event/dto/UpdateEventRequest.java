package com.schedule.api.event.dto;

import com.schedule.api.event.domain.EventSubjectType;
import java.time.LocalDate;

public record UpdateEventRequest(
        String title,
        LocalDate startDate,
        LocalDate endDate,
        EventSubjectType subjectType,
        String ownerUserId,
        String note
) {
}
