package com.schedule.api.shift.service;

import com.schedule.api.common.context.RequestContext;
import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.exception.ErrorCode;
import com.schedule.api.common.util.IdGenerator;
import com.schedule.api.common.util.YearMonthValidator;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        YearMonthValidator.validate(year, month);
        YearMonth yearMonth = YearMonth.of(year, month);

        List<ShiftResponse> items = shiftScheduleRepository.findActiveShiftsInRangeByOwnerUserId(
                        context.groupId(),
                        context.userId(),
                        yearMonth.atDay(1),
                        yearMonth.atEndOfMonth()
                )
                .stream()
                .map(shift -> toResponse(shift, context.userId()))
                .toList();

        return new ShiftMonthResponse(year, month, items);
    }

    public ShiftDateResponse getDateShift(RequestContext context, LocalDate date) {
        ShiftResponse item = shiftScheduleRepository
                .findByGroupIdAndOwnerUserIdAndDateAndDeletedAtIsNull(context.groupId(), context.userId(), date)
                .map(shift -> toResponse(shift, context.userId()))
                .orElse(null);

        return new ShiftDateResponse(date, item);
    }

    @Transactional
    public ShiftResponse upsertShift(RequestContext context, LocalDate date, UpsertShiftRequest request) {
        Instant now = Instant.now();

        ShiftSchedule shift = shiftScheduleRepository
                .findByGroupIdAndOwnerUserIdAndDateAndDeletedAtIsNull(context.groupId(), context.userId(), date)
                .map(existing -> {
                    existing.update(request.shiftType(), context.userId(), now);
                    return existing;
                })
                .orElseGet(() -> new ShiftSchedule(
                        idGenerator.generate("sft_"),
                        context.groupId(),
                        date,
                        context.userId(),
                        request.shiftType(),
                        context.userId(),
                        context.userId(),
                        now,
                        now,
                        null
                ));

        return toResponse(shiftScheduleRepository.save(shift), context.userId());
    }

    @Transactional
    public MonthlyShiftResponse replaceMonthlyShifts(
            RequestContext context,
            int year,
            int month,
            MonthlyShiftUpsertRequest request
    ) {
        YearMonthValidator.validate(year, month);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        validateMonthlyItems(yearMonth, request.items());

        Instant now = Instant.now();
        List<ShiftSchedule> existingShifts = shiftScheduleRepository.findActiveShiftsInRangeByOwnerUserId(
                context.groupId(),
                context.userId(),
                startDate,
                endDate
        );
        for (ShiftSchedule existingShift : existingShifts) {
            existingShift.softDelete(context.userId(), now);
        }

        List<ShiftSchedule> newShifts = request.items()
                .stream()
                .map(item -> createShift(context, item, now))
                .toList();
        List<ShiftResponse> savedItems = shiftScheduleRepository.saveAll(newShifts)
                .stream()
                .map(shift -> toResponse(shift, context.userId()))
                .toList();

        return new MonthlyShiftResponse(year, month, existingShifts.size(), savedItems);
    }

    @Transactional
    public DeleteShiftResponse deleteShift(RequestContext context, LocalDate date) {
        ShiftSchedule shift = shiftScheduleRepository
                .findByGroupIdAndOwnerUserIdAndDateAndDeletedAtIsNull(context.groupId(), context.userId(), date)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIFT_NOT_FOUND));

        Instant deletedAt = Instant.now();
        shift.softDelete(context.userId(), deletedAt);

        return new DeleteShiftResponse(date, true, deletedAt);
    }

    private ShiftResponse toResponse(ShiftSchedule shiftSchedule, String currentUserId) {
        return new ShiftResponse(
                shiftSchedule.getId(),
                shiftSchedule.getGroupId(),
                shiftSchedule.getDate(),
                shiftSchedule.getOwnerUserId(),
                resolveOwnerType(shiftSchedule, currentUserId),
                shiftSchedule.getShiftType(),
                shiftSchedule.getCreatedByUserId(),
                shiftSchedule.getUpdatedByUserId(),
                shiftSchedule.getCreatedAt(),
                shiftSchedule.getUpdatedAt(),
                shiftSchedule.getDeletedAt()
        );
    }

    private String resolveOwnerType(ShiftSchedule shiftSchedule, String currentUserId) {
        return shiftSchedule.getOwnerUserId().equals(currentUserId) ? "ME" : "PARTNER";
    }

    private void validateMonthlyItems(YearMonth yearMonth, List<MonthlyShiftItemRequest> items) {
        Set<LocalDate> seenDates = new HashSet<>();
        for (MonthlyShiftItemRequest item : items) {
            if (!YearMonth.from(item.date()).equals(yearMonth)) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "all items must belong to the requested year and month"
                );
            }

            if (!seenDates.add(item.date())) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "items must not contain duplicate dates"
                );
            }
        }
    }

    private ShiftSchedule createShift(RequestContext context, MonthlyShiftItemRequest item, Instant now) {
        return new ShiftSchedule(
                idGenerator.generate("sft_"),
                context.groupId(),
                item.date(),
                context.userId(),
                item.shiftType(),
                context.userId(),
                context.userId(),
                now,
                now,
                null
        );
    }
}
