package com.schedule.api.shift.dto;

import java.util.List;

public record ShiftImagePreviewResponse(
        int year,
        int month,
        int daysInMonth,
        int recognizedCount,
        int issueCount,
        List<ShiftImagePreviewItemResponse> items,
        List<ShiftImagePreviewIssueResponse> issues,
        MonthlyShiftUpsertRequest monthlyUpsertRequest
) {
}
