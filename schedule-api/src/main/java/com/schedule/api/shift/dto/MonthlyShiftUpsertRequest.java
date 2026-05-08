package com.schedule.api.shift.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MonthlyShiftUpsertRequest(
        @NotNull(message = "근무 목록을 입력해야 합니다.") @Valid List<MonthlyShiftItemRequest> items
) {
}
