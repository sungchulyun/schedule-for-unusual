package com.schedule.api.shift.dto;

public record ShiftImagePreviewIssueResponse(
        Integer day,
        String rawCode,
        String message
) {
}
