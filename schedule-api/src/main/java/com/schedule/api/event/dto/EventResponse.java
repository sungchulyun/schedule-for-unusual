package com.schedule.api.event.dto;

import com.schedule.api.event.domain.EventOwnerType;
import com.schedule.api.event.domain.EventSubjectType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record EventResponse(
        String id,
        String groupId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        EventSubjectType subjectType,
        String ownerUserId,
        EventOwnerType ownerType,
        String note,
        String createdByUserId,
        String updatedByUserId,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
}
