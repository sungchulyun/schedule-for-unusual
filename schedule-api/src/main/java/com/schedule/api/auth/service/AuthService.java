package com.schedule.api.auth.service;

import com.schedule.api.auth.client.KakaoOAuthClient;
import com.schedule.api.auth.client.KakaoUserProfile;
import com.schedule.api.auth.config.AuthProperties;
import com.schedule.api.auth.domain.AppUser;
import com.schedule.api.auth.domain.OAuthProvider;
import com.schedule.api.auth.domain.RefreshToken;
import com.schedule.api.auth.domain.UserStatus;
import com.schedule.api.auth.dto.AuthResultResponse;
import com.schedule.api.auth.dto.AuthTokenResponse;
import com.schedule.api.auth.dto.LogoutResponse;
import com.schedule.api.auth.dto.UserProfileResponse;
import com.schedule.api.auth.repository.AppUserRepository;
import com.schedule.api.auth.repository.RefreshTokenRepository;
import com.schedule.api.auth.security.AuthenticatedUser;
import com.schedule.api.auth.security.JwtTokenProvider;
import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.exception.ErrorCode;
import com.schedule.api.common.util.IdGenerator;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final IdGenerator idGenerator;
    private final AuthProperties authProperties;

    public AuthService(
            KakaoOAuthClient kakaoOAuthClient,
            AppUserRepository appUserRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            IdGenerator idGenerator,
            AuthProperties authProperties
    ) {
        this.kakaoOAuthClient = kakaoOAuthClient;
        this.appUserRepository = appUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.idGenerator = idGenerator;
        this.authProperties = authProperties;
    }

    public String buildKakaoLoginUrl() {
        return authProperties.getKakao().getAuthUri()
                + "?response_type=code"
                + "&client_id=" + urlEncode(authProperties.getKakao().getClientId())
                + "&redirect_uri=" + urlEncode(authProperties.getKakao().getRedirectUri());
    }

    @Transactional
    public AuthResultResponse authenticateWithKakao(String authorizationCode) {
        KakaoUserProfile kakaoUserProfile = kakaoOAuthClient.getUserProfile(authorizationCode);

        AppUser user = appUserRepository.findByOauthProviderAndOauthProviderUserId(OAuthProvider.KAKAO, kakaoUserProfile.id())
                .orElse(null);
        boolean isNewUser = false;

        if (user == null) {
            Instant now = Instant.now();
            user = new AppUser(
                    idGenerator.generate("usr_"),
                    OAuthProvider.KAKAO,
                    kakaoUserProfile.id(),
                    kakaoUserProfile.nickname(),
                    kakaoUserProfile.profileImageUrl(),
                    idGenerator.generate("grp_"),
                    UserStatus.ACTIVE,
                    now,
                    now
            );
            user = appUserRepository.save(user);
            isNewUser = true;
        } else {
            user.updateProfile(kakaoUserProfile.nickname(), kakaoUserProfile.profileImageUrl(), Instant.now());
        }

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getId(), user.getGroupId(), user.getNickname());
        AuthTokenResponse tokens = issueTokens(authenticatedUser);
        return new AuthResultResponse(toProfile(user), tokens, isNewUser);
    }

    @Transactional
    public AuthTokenResponse refresh(String refreshToken) {
        JwtTokenProvider.RefreshClaims claims;
        try {
            claims = jwtTokenProvider.parseRefreshToken(refreshToken);
        } catch (io.jsonwebtoken.ExpiredJwtException exception) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED, "Refresh token expired");
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "Invalid refresh token");
        }

        RefreshToken savedToken = refreshTokenRepository.findByTokenKey(claims.tokenKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "Refresh token not found"));

        if (savedToken.getRevokedAt() != null) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_REVOKED, "Refresh token revoked");
        }

        if (savedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED, "Refresh token expired");
        }

        AppUser user = appUserRepository.findById(savedToken.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED, "User not found"));

        savedToken.revoke(Instant.now());
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getId(), user.getGroupId(), user.getNickname());
        return issueTokens(authenticatedUser);
    }

    @Transactional
    public LogoutResponse logout(String refreshToken) {
        JwtTokenProvider.RefreshClaims claims;
        try {
            claims = jwtTokenProvider.parseRefreshToken(refreshToken);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "Invalid refresh token");
        }

        RefreshToken savedToken = refreshTokenRepository.findByTokenKey(claims.tokenKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "Refresh token not found"));
        savedToken.revoke(Instant.now());

        return new LogoutResponse(true);
    }

    public UserProfileResponse getMyProfile(AuthenticatedUser authenticatedUser) {
        AppUser user = appUserRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED, "User not found"));
        return toProfile(user);
    }

    private AuthTokenResponse issueTokens(AuthenticatedUser authenticatedUser) {
        String accessToken = jwtTokenProvider.createAccessToken(authenticatedUser);
        JwtTokenProvider.RefreshTokenPayload refreshTokenPayload = jwtTokenProvider.createRefreshToken(authenticatedUser);

        refreshTokenRepository.save(new RefreshToken(
                idGenerator.generate("rtk_"),
                authenticatedUser.userId(),
                refreshTokenPayload.tokenKey(),
                refreshTokenPayload.expiresAt(),
                null,
                Instant.now()
        ));

        return new AuthTokenResponse(
                accessToken,
                refreshTokenPayload.token(),
                "Bearer",
                jwtTokenProvider.getAccessTokenExpirySeconds(),
                jwtTokenProvider.getRefreshTokenExpirySeconds()
        );
    }

    private UserProfileResponse toProfile(AppUser user) {
        return new UserProfileResponse(
                user.getId(),
                user.getOauthProvider(),
                user.getOauthProviderUserId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getGroupId(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
