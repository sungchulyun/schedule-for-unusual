package com.schedule.api.common.util;

import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.exception.ErrorCode;

public final class YearMonthValidator {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;

    private YearMonthValidator() {
    }

    public static void validate(int year, int month) {
        if (month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "month must be between 1 and 12");
        }

        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "year must be between 2000 and 2100");
        }
    }
}
