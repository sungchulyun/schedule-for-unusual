package com.schedule.api.auth.exception;

import com.schedule.api.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {

    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    AUTH_REFRESH_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 무효화되었습니다."),
    AUTH_KAKAO_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "카카오 로그인에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    AuthErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
