package com.schedule.api.auth.controller;

import com.schedule.api.auth.dto.UpdateUserSettingsRequest;
import com.schedule.api.auth.security.AuthenticatedUser;
import com.schedule.api.auth.service.AuthService;
import com.schedule.api.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ApiResponse<?> me(Authentication authentication) {
        return ApiResponse.success(authService.getMyProfile((AuthenticatedUser) authentication.getPrincipal()));
    }

    @PatchMapping("/me/settings")
    public ApiResponse<?> updateSettings(
            Authentication authentication,
            @Valid @RequestBody UpdateUserSettingsRequest request
    ) {
        return ApiResponse.success(authService.updateMySettings(
                (AuthenticatedUser) authentication.getPrincipal(),
                request
        ));
    }
}
