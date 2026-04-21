package com.schedule.api.shift.service;

import com.schedule.api.common.context.RequestContext;
import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.exception.ErrorCode;
import com.schedule.api.common.util.IdGenerator;
import com.schedule.api.shift.dto.DeleteShiftResponse;
import com.schedule.api.shift.dto.MonthlyShiftItemRequest;
import com.schedule.api.shift.dto.MonthlyShiftResponse;
import com.schedule.api.shift.dto.MonthlyShiftUpsertRequest;
import com.schedule.api.shift.dto.ShiftDateResponse;
import com.schedule.api.shift.dto.ShiftMonthResponse;
import com.schedule.api.shift.dto.ShiftResponse;
import com.schedule.api.shift.dto.UpsertShiftRequest;
import com.schedule.api.shift.domain.ShiftSchedule;
import com.schedule.api.shift.repository.ShiftScheduleRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ShiftService {

    private final ShiftScheduleRepository shiftScheduleRepository;
    private final IdGenerator idGenerator;

    public ShiftService(ShiftScheduleRepository shiftScheduleRepository, IdGenerator idGenerator) {
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.idGenerator = idGenerator;
    }

    public ShiftMonthResponse getMonthlyShifts(RequestContext context, int year, int month) {
        validateYearMonth(year, month);
        YearMonth yearMonth = YearMonth.of(year, month);

        List<ShiftResponse> items = shiftScheduleRepository.findActiveShiftsInRange(
                        context.groupId(),
                        yearMonth.atDay(1),
                        yearMonth.atEndOfMonth()
                )
                .stream()
                .map(this::toResponse)
                .toList();

        return new ShiftMonthResponse(year, month, items);
    }

    public ShiftDateResponse getDateShift(RequestContext context, LocalDate date) {
        ShiftResponse item = shiftScheduleRepository.findByGroupIdAndDateAndDeletedAtIsNull(context.groupId(), date)
                .map(this::toResponse)
                .orElse(null);

        return new ShiftDateResponse(date, item);
    }

    @Transactional
    public ShiftResponse upsertShift(RequestContext context, LocalDate date, UpsertShiftRequest request) {
        Instant now = Instant.now();

        ShiftSchedule shift = shiftScheduleRepository.findByGroupIdAndDateAndDeletedAtIsNull(context.groupId(), date)
                .map(existing -> {
                    existing.update(request.shiftType(), context.userId(), now);
                    return existing;
                })
                .orElseGet(() -> new ShiftSchedule(
                        idGenerator.generate("sft_"),
                        context.groupId(),
                        date,
                        request.shiftType(),
                        context.userId(),
                        context.userId(),
                        now,
                        now,
                        null
                ));

        return toResponse(shiftScheduleRepository.save(shift));
    }

    @Transactional
    public MonthlyShiftResponse replaceMonthlyShifts(
            RequestContext context,
            int year,
            int month,
            MonthlyShiftUpsertRequest request
    ) {
        validateYearMonth(year, month);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        for (MonthlyShiftItemRequest item : request.items()) {
            if (!YearMonth.from(item.date()).equals(yearMonth)) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "all items must belong to the requested year and month"
                );
            }
        }

        Instant now = Instant.now();
        List<ShiftSchedule> existingShifts = shiftScheduleRepository.findActiveShiftsInRange(context.groupId(), startDate, endDate);
        for (ShiftSchedule existingShift : existingShifts) {
            existingShift.softDelete(context.userId(), now);
        }

        List<ShiftResponse> savedItems = new ArrayList<>();
        for (MonthlyShiftItemRequest item : request.items()) {
            ShiftSchedule shift = new ShiftSchedule(
                    idGenerator.generate("sft_"),
                    context.groupId(),
                    item.date(),
                    item.shiftType(),
                    context.userId(),
                    context.userId(),
                    now,
                    now,
                    null
            );
            savedItems.add(toResponse(shiftScheduleRepository.save(shift)));
        }

        return new MonthlyShiftResponse(year, month, existingShifts.size(), savedItems);
    }

    @Transactional
    public DeleteShiftResponse deleteShift(RequestContext context, LocalDate date) {
        ShiftSchedule shift = shiftScheduleRepository.findByGroupIdAndDateAndDeletedAtIsNull(context.groupId(), date)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIFT_NOT_FOUND));

        Instant deletedAt = Instant.now();
        shift.softDelete(context.userId(), deletedAt);

        return new DeleteShiftResponse(date, true, deletedAt);
    }

    private void validateYearMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "month must be between 1 and 12");
        }

        if (year < 2000 || year > 2100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "year must be between 2000 and 2100");
        }
    }

    private ShiftResponse toResponse(ShiftSchedule shiftSchedule) {
        return new ShiftResponse(
                shiftSchedule.getId(),
                shiftSchedule.getGroupId(),
                shiftSchedule.getDate(),
                shiftSchedule.getShiftType(),
                shiftSchedule.getCreatedByUserId(),
                shiftSchedule.getUpdatedByUserId(),
                shiftSchedule.getCreatedAt(),
                shiftSchedule.getUpdatedAt(),
                shiftSchedule.getDeletedAt()
        );
    }
}
