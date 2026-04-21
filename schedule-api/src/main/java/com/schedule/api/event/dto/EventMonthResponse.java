package com.schedule.api.event.dto;

import java.util.List;

public record EventMonthResponse(
        int year,
        int month,
        List<EventResponse> items
) {
}
