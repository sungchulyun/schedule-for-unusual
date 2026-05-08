package com.schedule.api.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    default String getCode() {
        return ((Enum<?>) this).name();
    }

    HttpStatus getHttpStatus();

    String getDefaultMessage();
}
