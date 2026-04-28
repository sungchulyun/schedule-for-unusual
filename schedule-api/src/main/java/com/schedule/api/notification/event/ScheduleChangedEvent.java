package com.schedule.api.notification.event;

import java.time.LocalDate;

public record ScheduleChangedEvent(
        String groupId,
        String actorUserId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        ScheduleChangeType changeType
) {
}
