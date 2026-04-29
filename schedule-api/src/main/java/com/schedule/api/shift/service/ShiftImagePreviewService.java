package com.schedule.api.shift.service;

import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.exception.ErrorCode;
import com.schedule.api.common.util.YearMonthValidator;
import com.schedule.api.shift.domain.ShiftType;
import com.schedule.api.shift.dto.MonthlyShiftItemRequest;
import com.schedule.api.shift.dto.MonthlyShiftUpsertRequest;
import com.schedule.api.shift.dto.ShiftImagePreviewIssueResponse;
import com.schedule.api.shift.dto.ShiftImagePreviewItemResponse;
import com.schedule.api.shift.dto.ShiftImagePreviewResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShiftImagePreviewService {

    private static final Set<Character> IGNORED_CODES = Set.of('|', ',', '.', ';', ':', '-', '_');
    private static final Map<String, ShiftType> SHIFT_TYPES_BY_CODE = Map.of(
            "/", ShiftType.OFF,
            "D", ShiftType.DAY,
            "E", ShiftType.EVENING,
            "N", ShiftType.NIGHT,
            "M", ShiftType.MID
    );

    public ShiftImagePreviewResponse preview(int year, int month, String scheduleText) {
        YearMonthValidator.validate(year, month);
        if (!StringUtils.hasText(scheduleText)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "scheduleText must not be blank");
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();
        ParseResult parseResult = parseScheduleCodes(scheduleText);
        List<ParsedShiftCode> parsedCodes = parseResult.parsedCodes();
        List<ShiftImagePreviewItemResponse> items = new ArrayList<>();
        List<ShiftImagePreviewIssueResponse> issues = new ArrayList<>(parseResult.issues());
        List<MonthlyShiftItemRequest> upsertItems = new ArrayList<>();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = yearMonth.atDay(day);
            if (day > parsedCodes.size()) {
                issues.add(new ShiftImagePreviewIssueResponse(day, null, "recognized schedule code is missing"));
                continue;
            }

            ParsedShiftCode parsedCode = parsedCodes.get(day - 1);
            ShiftType shiftType = SHIFT_TYPES_BY_CODE.get(parsedCode.normalizedCode());
            items.add(new ShiftImagePreviewItemResponse(
                    date,
                    day,
                    parsedCode.rawCode(),
                    parsedCode.normalizedCode(),
                    shiftType,
                    true
            ));
            upsertItems.add(new MonthlyShiftItemRequest(date, shiftType));
        }

        if (parsedCodes.size() > daysInMonth) {
            for (int index = daysInMonth; index < parsedCodes.size(); index++) {
                ParsedShiftCode parsedCode = parsedCodes.get(index);
                issues.add(new ShiftImagePreviewIssueResponse(
                        null,
                        parsedCode.rawCode(),
                        "recognized schedule code exceeds requested month length"
                ));
            }
        }

        return new ShiftImagePreviewResponse(
                year,
                month,
                daysInMonth,
                items.size(),
                issues.size(),
                items,
                issues,
                new MonthlyShiftUpsertRequest(upsertItems)
        );
    }

    private ParseResult parseScheduleCodes(String scheduleText) {
        List<ParsedShiftCode> parsedCodes = new ArrayList<>();
        List<ShiftImagePreviewIssueResponse> issues = new ArrayList<>();

        for (int index = 0; index < scheduleText.length(); index++) {
            char code = scheduleText.charAt(index);
            String rawCode = String.valueOf(code);
            String normalizedCode = normalizeCode(code);

            if (normalizedCode != null) {
                parsedCodes.add(new ParsedShiftCode(rawCode, normalizedCode));
                continue;
            }

            if (shouldIgnore(code)) {
                continue;
            }

            issues.add(new ShiftImagePreviewIssueResponse(null, rawCode, "unsupported schedule code"));
        }

        return new ParseResult(parsedCodes, issues);
    }

    private String normalizeCode(char code) {
        String normalizedCode = String.valueOf(code).toUpperCase();
        return SHIFT_TYPES_BY_CODE.containsKey(normalizedCode) ? normalizedCode : null;
    }

    private boolean shouldIgnore(char code) {
        return Character.isWhitespace(code)
                || Character.isDigit(code)
                || IGNORED_CODES.contains(code);
    }

    private record ParsedShiftCode(
            String rawCode,
            String normalizedCode
    ) {
    }

    private record ParseResult(
            List<ParsedShiftCode> parsedCodes,
            List<ShiftImagePreviewIssueResponse> issues
    ) {
    }
}
