package com.schedule.api.common.response;

public record ApiError(
        String code,
        String message
) {
}
