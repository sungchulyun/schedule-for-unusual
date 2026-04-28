package com.schedule.api.shift.dto;

import com.schedule.api.shift.domain.ShiftType;
import java.time.Instant;
import java.time.LocalDate;

public record ShiftResponse(
        String id,
        String groupId,
        LocalDate date,
        String ownerUserId,
        String ownerType,
        ShiftType shiftType,
        String createdByUserId,
        String updatedByUserId,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
}
