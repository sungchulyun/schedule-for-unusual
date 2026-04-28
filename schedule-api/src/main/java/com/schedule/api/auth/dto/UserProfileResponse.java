package com.schedule.api.auth.dto;

import com.schedule.api.auth.domain.DefaultShiftOwnerType;
import com.schedule.api.auth.domain.OAuthProvider;
import java.time.Instant;

public record UserProfileResponse(
        String id,
        OAuthProvider oauthProvider,
        String oauthProviderUserId,
        String nickname,
        String profileImageUrl,
        String groupId,
        DefaultShiftOwnerType defaultShiftOwnerType,
        Instant createdAt,
        Instant updatedAt
) {
}
