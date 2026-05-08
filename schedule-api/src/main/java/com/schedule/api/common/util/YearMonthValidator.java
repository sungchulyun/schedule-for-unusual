package com.schedule.api.common.util;


import com.schedule.api.common.exception.CommonErrorCode;
import com.schedule.api.common.exception.BusinessException;

public final class YearMonthValidator {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;

    private YearMonthValidator() {
    }

    public static void validate(int year, int month) {
        if (month < 1 || month > 12) {
            throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "month는 1에서 12 사이여야 합니다.");
        }

        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "year는 2000에서 2100 사이여야 합니다.");
        }
    }
}
