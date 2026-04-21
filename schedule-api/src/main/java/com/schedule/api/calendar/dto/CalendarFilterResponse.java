package com.schedule.api.calendar.dto;

import java.util.List;

public record CalendarFilterResponse(
        List<String> ownerTypes,
        boolean includeShifts
) {
}
