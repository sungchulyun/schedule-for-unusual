package com.schedule.api.event.support;

import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.exception.ErrorCode;
import com.schedule.api.event.domain.EventOwnerType;
import java.util.Arrays;
import java.util.List;

public final class EventOwnerTypeFilterParser {

    private EventOwnerTypeFilterParser() {
    }

    public static List<EventOwnerType> parse(String ownerTypes) {
        if (ownerTypes == null || ownerTypes.isBlank()) {
            return List.of();
        }

        try {
            return Arrays.stream(ownerTypes.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(String::toUpperCase)
                    .map(EventOwnerType::valueOf)
                    .distinct()
                    .toList();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "ownerTypes must contain only ME, US, PARTNER"
            );
        }
    }
}
