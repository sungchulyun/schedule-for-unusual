package com.schedule.api.event.dto;

import java.time.Instant;

public record DeleteEventResponse(
        String id,
        boolean deleted,
        Instant deletedAt
) {
}
