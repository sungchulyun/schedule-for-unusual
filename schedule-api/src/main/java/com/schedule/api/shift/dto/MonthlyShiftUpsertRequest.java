package com.schedule.api.shift.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MonthlyShiftUpsertRequest(
        @NotNull @Valid List<MonthlyShiftItemRequest> items
) {
}
