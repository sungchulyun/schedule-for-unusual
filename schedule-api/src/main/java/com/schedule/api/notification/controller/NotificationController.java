package com.schedule.api.notification.controller;

import com.schedule.api.common.context.RequestContextProvider;
import com.schedule.api.common.response.ApiResponse;
import com.schedule.api.notification.dto.FcmTokenRequest;
import com.schedule.api.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final RequestContextProvider contextProvider;
    private final NotificationService notificationService;

    public NotificationController(
            RequestContextProvider contextProvider,
            NotificationService notificationService
    ) {
        this.contextProvider = contextProvider;
        this.notificationService = notificationService;
    }

    @PostMapping("/fcm-token")
    public ApiResponse<?> registerToken(@Valid @RequestBody FcmTokenRequest request) {
        return ApiResponse.success(notificationService.registerToken(contextProvider.getRequiredContext(), request));
    }

    @DeleteMapping("/fcm-token")
    public ApiResponse<Void> unregisterToken(@Valid @RequestBody FcmTokenRequest request) {
        notificationService.unregisterToken(contextProvider.getRequiredContext(), request.token());
        return ApiResponse.empty();
    }
}
