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
import com.schedule.api.auth.dto.UpdateUserSettingsRequest;
import com.schedule.api.auth.dto.UserProfileResponse;
import com.schedule.api.auth.repository.AppUserRepository;
import com.schedule.api.auth.repository.RefreshTokenRepository;
import com.schedule.api.auth.security.AuthenticatedUser;
import com.schedule.api.auth.security.JwtTokenProvider;
import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.exception.ErrorCode;
import com.schedule.api.common.util.IdGenerator;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Transactional(readOnly = true)
public class AuthService {
    private static final String ALLOWED_APP_REDIRECT_PREFIX = "scheduleapp://auth/callback";
    private static final long OAUTH_STATE_TTL_SECONDS = 300;
    private static final long MOBILE_LOGIN_CODE_TTL_SECONDS = 300;

    private final KakaoOAuthClient kakaoOAuthClient;
    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final IdGenerator idGenerator;
    private final AuthProperties authProperties;
    private final Map<String, PendingAppLogin> pendingAppLogins = new ConcurrentHashMap<>();
    private final Map<String, PendingMobileLogin> pendingMobileLogins = new ConcurrentHashMap<>();

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

    public String buildKakaoLoginUrl(String appRedirectUri) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(authProperties.getKakao().getAuthUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", authProperties.getKakao().getClientId())
                .queryParam("redirect_uri", authProperties.getKakao().getRedirectUri());

        String validatedRedirectUri = validateAppRedirectUri(appRedirectUri);
        builder.queryParam("state", issueLoginState(validatedRedirectUri));

        return builder.build().toUriString();
    }

    @Transactional
    public AuthResultResponse exchangeMobileLogin(String loginCode) {
        if (loginCode == null || loginCode.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Login code is required");
        }

        PendingMobileLogin pendingMobileLogin = pendingMobileLogins.remove(loginCode.trim());
        if (pendingMobileLogin == null || pendingMobileLogin.expiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "Login code is invalid or expired");
        }

        return pendingMobileLogin.authResult();
    }

    public String consumeAppRedirectUri(String state) {
        if (state == null || state.isBlank()) {
            return null;
        }

        PendingAppLogin pendingAppLogin = pendingAppLogins.remove(state.trim());
        if (pendingAppLogin == null || pendingAppLogin.expiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "OAuth state is invalid or expired");
        }

        return pendingAppLogin.appRedirectUri();
    }

    public String buildAppRedirectUrl(String appRedirectUri, AuthResultResponse authResult) {
        String validatedRedirectUri = validateAppRedirectUri(appRedirectUri);

        return UriComponentsBuilder.fromUriString(validatedRedirectUri)
                .queryParam("loginCode", issueMobileLoginCode(authResult))
                .queryParam("isNewUser", authResult.isNewUser())
                .build()
                .toUriString();
    }

    public String buildAppErrorRedirectUrl(String appRedirectUri, String error, String errorDescription) {
        String validatedRedirectUri = validateAppRedirectUri(appRedirectUri);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(validatedRedirectUri)
                .queryParam("errorCode", ErrorCode.AUTH_KAKAO_LOGIN_FAILED.name())
                .queryParam("error", error);

        if (errorDescription != null && !errorDescription.isBlank()) {
            builder.queryParam("errorDescription", errorDescription);
        }

        return builder.build().toUriString();
    }

    @Transactional
    public AuthResultResponse authenticateWithKakao(String authorizationCode) {
        return authenticateKakaoUser(kakaoOAuthClient.getUserProfile(authorizationCode));
    }

    @Transactional
    public AuthResultResponse authenticateWithKakaoAccessToken(String accessToken) {
        return authenticateKakaoUser(kakaoOAuthClient.getUserProfileByAccessToken(accessToken));
    }

    @Transactional
    private AuthResultResponse authenticateKakaoUser(KakaoUserProfile kakaoUserProfile) {
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
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED, "User is not active");
        }

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

    @Transactional
    public UserProfileResponse updateMySettings(AuthenticatedUser authenticatedUser, UpdateUserSettingsRequest request) {
        AppUser user = appUserRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED, "User not found"));
        user.updateDefaultShiftOwnerType(request.defaultShiftOwnerType(), Instant.now());
        return toProfile(user);
    }

    public AuthTokenResponse issueTokensForUser(AppUser user) {
        return issueTokens(new AuthenticatedUser(user.getId(), user.getGroupId(), user.getNickname()));
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
                user.getDefaultShiftOwnerType(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String validateAppRedirectUri(String appRedirectUri) {
        if (appRedirectUri == null || appRedirectUri.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "App redirect URI is required");
        }

        String normalized = appRedirectUri.trim();
        if (!ALLOWED_APP_REDIRECT_PREFIX.equals(normalized)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported app redirect URI");
        }
        return normalized;
    }

    private String issueLoginState(String appRedirectUri) {
        cleanupExpiredEntries();

        String state = UUID.randomUUID().toString();
        pendingAppLogins.put(state, new PendingAppLogin(appRedirectUri, Instant.now().plusSeconds(OAUTH_STATE_TTL_SECONDS)));
        return state;
    }

    private String issueMobileLoginCode(AuthResultResponse authResult) {
        cleanupExpiredEntries();

        String loginCode = UUID.randomUUID().toString();
        pendingMobileLogins.put(loginCode, new PendingMobileLogin(authResult, Instant.now().plusSeconds(MOBILE_LOGIN_CODE_TTL_SECONDS)));
        return loginCode;
    }

    private void cleanupExpiredEntries() {
        Instant now = Instant.now();
        pendingAppLogins.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        pendingMobileLogins.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record PendingAppLogin(
            String appRedirectUri,
            Instant expiresAt
    ) {
    }

    private record PendingMobileLogin(
            AuthResultResponse authResult,
            Instant expiresAt
    ) {
    }
}
