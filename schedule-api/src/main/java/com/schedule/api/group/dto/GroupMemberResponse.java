package com.schedule.api.group.dto;

public record GroupMemberResponse(
        String userId,
        String role,
        String partnerStatus
) {
}
