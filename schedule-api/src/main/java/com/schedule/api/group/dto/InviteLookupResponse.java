package com.schedule.api.group.dto;

import java.time.Instant;

public record InviteLookupResponse(
        String inviteId,
        String groupId,
        InviteInviterResponse inviter,
        String status,
        boolean requiresAuth,
        Instant expiresAt
) {
}
