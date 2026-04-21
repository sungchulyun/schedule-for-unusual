package com.schedule.api.calendar.service;

import com.schedule.api.calendar.dto.CalendarDateResponse;
import com.schedule.api.calendar.dto.CalendarDayEventResponse;
import com.schedule.api.calendar.dto.CalendarDayShiftResponse;
import com.schedule.api.calendar.dto.CalendarDaySummaryResponse;
import com.schedule.api.calendar.dto.CalendarFilterResponse;
import com.schedule.api.calendar.dto.CalendarMetaResponse;
import com.schedule.api.calendar.dto.CalendarMonthResponse;
import com.schedule.api.common.context.RequestContext;
import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.exception.ErrorCode;
import com.schedule.api.event.domain.Event;
import com.schedule.api.event.domain.EventOwnerType;
import com.schedule.api.event.domain.EventSubjectType;
import com.schedule.api.event.dto.EventResponse;
import com.schedule.api.event.repository.EventRepository;
import com.schedule.api.group.service.GroupQueryService;
import com.schedule.api.shift.domain.ShiftSchedule;
import com.schedule.api.shift.dto.ShiftResponse;
import com.schedule.api.shift.repository.ShiftScheduleRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CalendarQueryService {

    private static final List<String> DEFAULT_OWNER_TYPES = List.of("ME", "US", "PARTNER");
    private static final List<String> DEFAULT_SHIFT_TYPES = List.of("DAY", "NIGHT", "MID", "EVENING", "OFF", "VACATION");

    private final EventRepository eventRepository;
    private final ShiftScheduleRepository shiftScheduleRepository;
    private final GroupQueryService groupQueryService;

    public CalendarQueryService(
            EventRepository eventRepository,
            ShiftScheduleRepository shiftScheduleRepository,
            GroupQueryService groupQueryService
    ) {
        this.eventRepository = eventRepository;
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.groupQueryService = groupQueryService;
    }

    public CalendarMonthResponse getMonthlyCalendar(RequestContext context, int year, int month) {
        validateYearMonth(year, month);
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<EventResponse> events = eventRepository.findActiveEventsInRange(context.groupId(), startDate, endDate)
                .stream()
                .map(event -> toEventResponse(event, context.userId()))
                .toList();

        List<ShiftResponse> shifts = shiftScheduleRepository.findActiveShiftsInRange(context.groupId(), startDate, endDate)
                .stream()
                .map(this::toShiftResponse)
                .toList();

        return new CalendarMonthResponse(
                year,
                month,
                new CalendarFilterResponse(DEFAULT_OWNER_TYPES, true),
                buildMeta(context),
                events,
                shifts,
                buildDaySummaries(yearMonth, events, shifts)
        );
    }

    public CalendarDateResponse getDateCalendar(RequestContext context, LocalDate date) {
        List<EventResponse> events = eventRepository.findActiveEventsInRange(context.groupId(), date, date)
                .stream()
                .map(event -> toEventResponse(event, context.userId()))
                .sorted(Comparator.comparing(EventResponse::startDate)
                        .thenComparing(EventResponse::endDate)
                        .thenComparing(EventResponse::createdAt))
                .toList();

        ShiftResponse shift = shiftScheduleRepository.findByGroupIdAndDateAndDeletedAtIsNull(context.groupId(), date)
                .map(this::toShiftResponse)
                .orElse(null);

        return new CalendarDateResponse(date, buildMeta(context), shift, events);
    }

    private EventResponse toEventResponse(Event event, String currentUserId) {
        return new EventResponse(
                event.getId(),
                event.getGroupId(),
                event.getTitle(),
                event.getStartDate(),
                event.getEndDate(),
                event.getSubjectType(),
                event.getOwnerUserId(),
                resolveOwnerType(event, currentUserId),
                event.getNote(),
                event.getCreatedByUserId(),
                event.getUpdatedByUserId(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                event.getDeletedAt()
        );
    }

    private ShiftResponse toShiftResponse(ShiftSchedule shiftSchedule) {
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

    private EventOwnerType resolveOwnerType(Event event, String currentUserId) {
        if (event.getSubjectType() == EventSubjectType.SHARED) {
            return EventOwnerType.US;
        }

        if (event.getOwnerUserId() != null && event.getOwnerUserId().equals(currentUserId)) {
            return EventOwnerType.ME;
        }

        return EventOwnerType.PARTNER;
    }

    private CalendarMetaResponse buildMeta(RequestContext context) {
        return new CalendarMetaResponse(
                context.groupId(),
                context.userId(),
                groupQueryService.toCalendarMembers(groupQueryService.loadGroupMembers(context.groupId())),
                DEFAULT_SHIFT_TYPES
        );
    }

    private List<CalendarDaySummaryResponse> buildDaySummaries(
            YearMonth yearMonth,
            List<EventResponse> events,
            List<ShiftResponse> shifts
    ) {
        Map<LocalDate, ShiftResponse> shiftByDate = shifts.stream()
                .collect(Collectors.toMap(ShiftResponse::date, Function.identity(), (left, right) -> right));

        List<CalendarDaySummaryResponse> days = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            ShiftResponse shift = shiftByDate.get(date);
            List<CalendarDayEventResponse> dayEvents = events.stream()
                    .filter(event -> !event.startDate().isAfter(date) && !event.endDate().isBefore(date))
                    .map(event -> new CalendarDayEventResponse(
                            event.id(),
                            event.title(),
                            event.subjectType(),
                            event.ownerUserId(),
                            event.ownerType(),
                            event.startDate(),
                            event.endDate(),
                            !event.startDate().equals(event.endDate())
                    ))
                    .toList();

            days.add(new CalendarDaySummaryResponse(
                    date,
                    shift == null ? null : new CalendarDayShiftResponse(shift.id(), shift.shiftType()),
                    dayEvents
            ));
        }
        return days;
    }

    private void validateYearMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "month must be between 1 and 12");
        }
        if (year < 1900 || year > 3000) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "year must be between 1900 and 3000");
        }
    }
}
