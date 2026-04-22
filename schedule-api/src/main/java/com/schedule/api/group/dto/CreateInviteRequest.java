package com.schedule.api.group.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateInviteRequest(
        @NotBlank String channel
) {
}
