package com.schedule.api.group.dto;

public record GroupPermissionsResponse(
        boolean canReadAllEvents,
        boolean canEditAllEvents,
        boolean canEditAllShifts
) {
}
