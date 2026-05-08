package com.schedule.api.event.exception;

import com.schedule.api.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum EventErrorCode implements ErrorCode {

    INVALID_EVENT_TITLE(HttpStatus.BAD_REQUEST, "제목을 입력해야 합니다."),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 일정입니다."),
    EVENT_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "일정 날짜 범위가 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    EventErrorCode(HttpStatus httpStatus, String defaultMessage) {
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
