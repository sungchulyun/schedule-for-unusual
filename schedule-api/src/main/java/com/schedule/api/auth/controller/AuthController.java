package com.schedule.api.auth.controller;

import com.schedule.api.auth.dto.AuthResultResponse;
import com.schedule.api.auth.dto.AuthTokenResponse;
import com.schedule.api.auth.dto.KakaoMobileLoginRequest;
import com.schedule.api.auth.dto.LogoutRequest;
import com.schedule.api.auth.dto.LogoutResponse;
import com.schedule.api.auth.dto.MobileLoginExchangeRequest;
import com.schedule.api.auth.dto.RefreshTokenRequest;
import com.schedule.api.auth.service.AuthService;
import com.schedule.api.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/kakao/login")
    public ResponseEntity<Void> redirectToKakaoLogin(@RequestParam(name = "appRedirectUri") String appRedirectUri) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authService.buildKakaoLoginUrl(appRedirectUri))
                .build();
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<?> kakaoCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription
    ) {
        if (error != null && !error.isBlank()) {
            String appRedirectUri = authService.consumeAppRedirectUri(state);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, authService.buildAppErrorRedirectUrl(appRedirectUri, error, errorDescription))
                    .build();
        }

        if (code == null || code.isBlank()) {
            throw new jakarta.validation.ValidationException("Authorization code is required");
        }

        AuthResultResponse result = authService.authenticateWithKakao(code);
        String appRedirectUri = authService.consumeAppRedirectUri(state);
        if (appRedirectUri != null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, authService.buildAppRedirectUrl(appRedirectUri, result))
                    .build();
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/kakao/mobile")
    public ApiResponse<AuthResultResponse> kakaoMobileLogin(@Valid @RequestBody KakaoMobileLoginRequest request) {
        return ApiResponse.success(authService.authenticateWithKakaoAccessToken(request.accessToken()));
    }

    @PostMapping("/mobile/exchange")
    public ApiResponse<AuthResultResponse> exchangeMobileLogin(@Valid @RequestBody MobileLoginExchangeRequest request) {
        return ApiResponse.success(authService.exchangeMobileLogin(request.loginCode()));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<LogoutResponse> logout(@Valid @RequestBody LogoutRequest request) {
        return ApiResponse.success(authService.logout(request.refreshToken()));
    }
}
