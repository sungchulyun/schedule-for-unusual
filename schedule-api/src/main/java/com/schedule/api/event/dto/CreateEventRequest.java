package com.schedule.api.event.dto;

import com.schedule.api.event.domain.EventSubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateEventRequest(
        @NotBlank(message = "제목을 입력해야 합니다.") String title,
        @NotNull(message = "시작 날짜를 입력해야 합니다.") LocalDate startDate,
        @NotNull(message = "종료 날짜를 입력해야 합니다.") LocalDate endDate,
        @NotNull(message = "시작 시간을 입력해야 합니다.") LocalTime startTime,
        @NotNull(message = "종료 시간을 입력해야 합니다.") LocalTime endTime,
        @NotNull(message = "일정 유형을 입력해야 합니다.") EventSubjectType subjectType,
        String ownerUserId,
        String note
) {
}
