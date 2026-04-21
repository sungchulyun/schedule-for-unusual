package com.schedule.api.calendar.dto;

import java.util.List;

public record CalendarMetaResponse(
        String groupId,
        String currentUserId,
        List<CalendarMetaMemberResponse> members,
        List<String> shiftTypes
) {
}
