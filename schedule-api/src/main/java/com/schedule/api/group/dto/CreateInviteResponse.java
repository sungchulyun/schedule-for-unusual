package com.schedule.api.group.dto;

import java.time.Instant;

public record CreateInviteResponse(
        String groupId,
        String inviteCode,
        Instant expiresAt
) {
}
