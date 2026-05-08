package com.schedule.api.group.exception;

import com.schedule.api.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum GroupErrorCode implements ErrorCode {

    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 그룹입니다."),
    GROUP_ACCESS_DENIED(HttpStatus.FORBIDDEN, "그룹에 접근할 수 없습니다."),
    GROUP_PARTNER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 파트너가 연결된 그룹입니다."),
    GROUP_MEMBER_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "그룹 인원 제한을 초과했습니다."),
    GROUP_INVITE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 초대입니다."),
    GROUP_INVITE_EXPIRED(HttpStatus.BAD_REQUEST, "초대가 만료되었습니다."),
    INVALID_GROUP_INVITE_STATUS(HttpStatus.BAD_REQUEST, "초대 대기 상태가 아닙니다."),
    GROUP_SELF_INVITE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신은 초대할 수 없습니다."),
    USER_ALREADY_IN_GROUP(HttpStatus.CONFLICT, "사용자가 이미 다른 그룹에 속해 있습니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    GroupErrorCode(HttpStatus httpStatus, String defaultMessage) {
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
