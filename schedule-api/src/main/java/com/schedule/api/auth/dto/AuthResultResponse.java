package com.schedule.api.auth.dto;

public record AuthResultResponse(
        UserProfileResponse user,
        AuthTokenResponse tokens,
        boolean isNewUser
) {
}
