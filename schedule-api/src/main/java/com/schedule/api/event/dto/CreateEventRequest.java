package com.schedule.api.event.dto;

import com.schedule.api.event.domain.EventSubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateEventRequest(
        @NotBlank String title,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull EventSubjectType subjectType,
        String ownerUserId,
        String note
) {
}
