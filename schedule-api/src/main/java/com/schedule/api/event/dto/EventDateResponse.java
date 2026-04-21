package com.schedule.api.event.dto;

import java.time.LocalDate;
import java.util.List;

public record EventDateResponse(
        LocalDate date,
        List<EventResponse> items
) {
}
