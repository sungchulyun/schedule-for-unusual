package com.schedule.api.shift.dto;

import jakarta.validation.constraints.NotBlank;

public record ShiftImagePreviewTextRequest(
        @NotBlank String scheduleText
) {
}
