package com.schedule.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        ApiMetadata meta
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, ApiMetadata.now());
    }

    public static <T> ApiResponse<T> success(T data, ApiMetadata meta) {
        return new ApiResponse<>(true, data, null, meta);
    }

    public static ApiResponse<Void> empty() {
        return new ApiResponse<>(true, null, null, ApiMetadata.now());
    }

    public static ApiResponse<Void> empty(ApiMetadata meta) {
        return new ApiResponse<>(true, null, null, meta);
    }

    public static ApiResponse<Void> failure(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message), ApiMetadata.now());
    }

    public static ApiResponse<Void> failure(String code, String message, ApiMetadata meta) {
        return new ApiResponse<>(false, null, new ApiError(code, message), meta);
    }
}
