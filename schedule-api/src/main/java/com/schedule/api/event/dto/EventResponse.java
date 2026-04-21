package com.schedule.api.event.dto;

import com.schedule.api.event.domain.EventOwnerType;
import com.schedule.api.event.domain.EventSubjectType;
import java.time.Instant;
import java.time.LocalDate;

public record EventResponse(
        String id,
        String groupId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
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
