package com.schedule.api.auth.client;

public interface KakaoOAuthClient {

    KakaoUserProfile getUserProfile(String authorizationCode);
}
