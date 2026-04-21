package com.schedule.api.auth.client;

public record KakaoUserProfile(
        String id,
        String nickname,
        String profileImageUrl
) {
}
