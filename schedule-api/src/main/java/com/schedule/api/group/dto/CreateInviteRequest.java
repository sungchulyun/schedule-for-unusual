package com.schedule.api.group.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateInviteRequest(
        @NotBlank(message = "초대 채널을 입력해야 합니다.") String channel
) {
}
