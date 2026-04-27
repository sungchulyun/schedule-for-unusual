package com.schedule.api.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    AUTH_REFRESH_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 무효화되었습니다."),
    AUTH_KAKAO_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "카카오 로그인에 실패했습니다."),
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 그룹입니다."),
    GROUP_ACCESS_DENIED(HttpStatus.FORBIDDEN, "그룹에 접근할 수 없습니다."),
    GROUP_PARTNER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 파트너가 연결된 그룹입니다."),
    GROUP_MEMBER_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "그룹 인원 제한을 초과했습니다."),
    GROUP_INVITE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 초대입니다."),
    GROUP_INVITE_EXPIRED(HttpStatus.BAD_REQUEST, "초대가 만료되었습니다."),
    GROUP_SELF_INVITE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신은 초대할 수 없습니다."),
    USER_ALREADY_IN_GROUP(HttpStatus.CONFLICT, "사용자가 이미 다른 그룹에 속해 있습니다."),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 일정입니다."),
    EVENT_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "일정 날짜 범위가 올바르지 않습니다."),
    SHIFT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 근무 스케줄입니다."),
    SHIFT_INVALID_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 근무 타입입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
