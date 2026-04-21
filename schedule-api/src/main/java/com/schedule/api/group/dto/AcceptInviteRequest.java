package com.schedule.api.group.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptInviteRequest(
        @NotBlank String inviteCode
) {
}
