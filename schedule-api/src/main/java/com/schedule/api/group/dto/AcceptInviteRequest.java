package com.schedule.api.group.dto;

public record AcceptInviteRequest(
        String inviteCode,
        String inviteToken
) {
}
