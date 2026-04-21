package com.schedule.api.auth.controller;

import com.schedule.api.auth.dto.AuthResultResponse;
import com.schedule.api.auth.dto.AuthTokenResponse;
import com.schedule.api.auth.dto.LogoutRequest;
import com.schedule.api.auth.dto.LogoutResponse;
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
    public ResponseEntity<Void> redirectToKakaoLogin() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authService.buildKakaoLoginUrl())
                .build();
    }

    @GetMapping("/kakao/callback")
    public ApiResponse<AuthResultResponse> kakaoCallback(@RequestParam String code) {
        return ApiResponse.success(authService.authenticateWithKakao(code));
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
