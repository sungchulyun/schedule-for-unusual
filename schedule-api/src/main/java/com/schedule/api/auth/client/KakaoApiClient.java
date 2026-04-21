package com.schedule.api.auth.client;

import com.schedule.api.auth.config.AuthProperties;
import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.exception.ErrorCode;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoApiClient implements KakaoOAuthClient {

    private final RestClient restClient;
    private final AuthProperties authProperties;

    public KakaoApiClient(RestClient restClient, AuthProperties authProperties) {
        this.restClient = restClient;
        this.authProperties = authProperties;
    }

    @Override
    public KakaoUserProfile getUserProfile(String authorizationCode) {
        try {
            KakaoTokenResponse tokenResponse = exchangeAuthorizationCode(authorizationCode);
            KakaoUserResponse userResponse = restClient.get()
                    .uri(authProperties.getKakao().getUserInfoUri())
                    .header("Authorization", "Bearer " + tokenResponse.accessToken())
                    .retrieve()
                    .body(KakaoUserResponse.class);

            if (userResponse == null || userResponse.id() == null) {
                throw new BusinessException(ErrorCode.AUTH_KAKAO_LOGIN_FAILED, "Failed to read Kakao user profile");
            }

            String nickname = userResponse.kakaoAccount() != null && userResponse.kakaoAccount().profile() != null
                    ? userResponse.kakaoAccount().profile().nickname()
                    : "kakao-user";
            String profileImageUrl = userResponse.kakaoAccount() != null && userResponse.kakaoAccount().profile() != null
                    ? userResponse.kakaoAccount().profile().profileImageUrl()
                    : null;

            return new KakaoUserProfile(String.valueOf(userResponse.id()), nickname, profileImageUrl);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.AUTH_KAKAO_LOGIN_FAILED, "Kakao login failed");
        }
    }

    private KakaoTokenResponse exchangeAuthorizationCode(String authorizationCode) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", authProperties.getKakao().getClientId());
        body.add("redirect_uri", authProperties.getKakao().getRedirectUri());
        body.add("code", authorizationCode);

        if (authProperties.getKakao().getClientSecret() != null && !authProperties.getKakao().getClientSecret().isBlank()) {
            body.add("client_secret", authProperties.getKakao().getClientSecret());
        }

        KakaoTokenResponse response = restClient.post()
                .uri(authProperties.getKakao().getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(KakaoTokenResponse.class);

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_KAKAO_LOGIN_FAILED, "Failed to exchange Kakao authorization code");
        }

        return response;
    }

    private record KakaoTokenResponse(
            String tokenType,
            String accessToken
    ) {
    }

    private record KakaoUserResponse(
            Long id,
            KakaoAccount kakaoAccount
    ) {
    }

    private record KakaoAccount(
            Profile profile,
            Map<String, Object> properties
    ) {
    }

    private record Profile(
            String nickname,
            String profileImageUrl
    ) {
    }
}
