package com.schedule.api.shift.exception;

import com.schedule.api.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ShiftErrorCode implements ErrorCode {

    SHIFT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 근무 스케줄입니다."),
    SHIFT_INVALID_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 근무 타입입니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ShiftErrorCode(HttpStatus httpStatus, String defaultMessage) {
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
