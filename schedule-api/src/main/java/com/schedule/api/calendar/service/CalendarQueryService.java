package com.schedule.api.calendar.service;


import com.schedule.api.common.exception.CommonErrorCode;
import com.schedule.api.auth.exception.AuthErrorCode;
import com.schedule.api.auth.domain.AppUser;
import com.schedule.api.calendar.dto.CalendarDateResponse;
import com.schedule.api.calendar.dto.CalendarDayEventResponse;
import com.schedule.api.calendar.dto.CalendarDayShiftResponse;
import com.schedule.api.calendar.dto.CalendarDaySummaryResponse;
import com.schedule.api.calendar.dto.CalendarFilterResponse;
import com.schedule.api.calendar.dto.CalendarMetaResponse;
import com.schedule.api.calendar.dto.CalendarMonthResponse;
import com.schedule.api.common.context.RequestContext;
import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.util.YearMonthValidator;
import com.schedule.api.event.domain.Event;
import com.schedule.api.event.domain.EventOwnerType;
import com.schedule.api.event.domain.EventSubjectType;
import com.schedule.api.event.dto.EventResponse;
import com.schedule.api.event.repository.EventRepository;
import com.schedule.api.group.domain.GroupMembers;
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
import java.util.Set;
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

    public CalendarMonthResponse getMonthlyCalendar(
            RequestContext context,
            int year,
            int month,
            List<EventOwnerType> ownerTypes,
            boolean includeShifts,
            EventOwnerType shiftOwnerType
    ) {
        YearMonthValidator.validate(year, month);
        validateShiftOwnerType(shiftOwnerType);
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        GroupMembers members = groupQueryService.loadGroupMembers(context.groupId());
        EventOwnerType effectiveShiftOwnerType = resolveEffectiveShiftOwnerType(context, members.values(), shiftOwnerType);
        String shiftOwnerUserId = resolveShiftOwnerUserId(context, members.values(), effectiveShiftOwnerType);
        Set<EventOwnerType> allowedOwnerTypes = toAllowedOwnerTypes(ownerTypes);

        List<EventResponse> events = eventRepository.findActiveEventsInRange(context.groupId(), startDate, endDate)
                .stream()
                .map(event -> toEventResponse(event, context.userId()))
                .filter(event -> matchesOwnerTypes(event.ownerType(), allowedOwnerTypes))
                .toList();

        List<ShiftResponse> shifts = includeShifts
                ? findMonthlyShiftsForOwner(context, startDate, endDate, shiftOwnerUserId)
                        .stream()
                        .map(shift -> toShiftResponse(shift, context.userId()))
                        .sorted(shiftComparator(context.userId()))
                        .toList()
                : List.of();

        return new CalendarMonthResponse(
                year,
                month,
                new CalendarFilterResponse(toOwnerTypeNames(ownerTypes), includeShifts, effectiveShiftOwnerType.name()),
                buildMeta(context, members.values()),
                events,
                shifts,
                buildDaySummaries(yearMonth, events, shifts, includeShifts)
        );
    }

    public CalendarDateResponse getDateCalendar(
            RequestContext context,
            LocalDate date,
            List<EventOwnerType> ownerTypes,
            boolean includeShifts,
            EventOwnerType shiftOwnerType
    ) {
        validateShiftOwnerType(shiftOwnerType);
        GroupMembers members = groupQueryService.loadGroupMembers(context.groupId());
        EventOwnerType effectiveShiftOwnerType = resolveEffectiveShiftOwnerType(context, members.values(), shiftOwnerType);
        String shiftOwnerUserId = resolveShiftOwnerUserId(context, members.values(), effectiveShiftOwnerType);
        Set<EventOwnerType> allowedOwnerTypes = toAllowedOwnerTypes(ownerTypes);
        List<EventResponse> events = eventRepository.findActiveEventsInRange(context.groupId(), date, date)
                .stream()
                .map(event -> toEventResponse(event, context.userId()))
                .filter(event -> matchesOwnerTypes(event.ownerType(), allowedOwnerTypes))
                .sorted(Comparator.comparing(EventResponse::startDate)
                        .thenComparing(EventResponse::endDate)
                        .thenComparing(EventResponse::createdAt))
                .toList();

        List<ShiftResponse> shifts = includeShifts
                ? findDateShiftForOwner(context, date, shiftOwnerUserId)
                        .stream()
                        .map(item -> toShiftResponse(item, context.userId()))
                        .sorted(shiftComparator(context.userId()))
                        .toList()
                : List.of();
        ShiftResponse shift = shifts.stream()
                .findFirst()
                .orElse(null);

        return new CalendarDateResponse(date, buildMeta(context, members.values()), shift, shifts, events);
    }

    private EventResponse toEventResponse(Event event, String currentUserId) {
        return new EventResponse(
                event.getId(),
                event.getGroupId(),
                event.getTitle(),
                event.getStartDate(),
                event.getEndDate(),
                event.getStartTime(),
                event.getEndTime(),
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

    private ShiftResponse toShiftResponse(ShiftSchedule shiftSchedule, String currentUserId) {
        return new ShiftResponse(
                shiftSchedule.getId(),
                shiftSchedule.getGroupId(),
                shiftSchedule.getDate(),
                shiftSchedule.getOwnerUserId(),
                resolveShiftOwnerType(shiftSchedule, currentUserId),
                shiftSchedule.getShiftType(),
                shiftSchedule.getCreatedByUserId(),
                shiftSchedule.getUpdatedByUserId(),
                shiftSchedule.getCreatedAt(),
                shiftSchedule.getUpdatedAt(),
                shiftSchedule.getDeletedAt()
        );
    }

    private String resolveShiftOwnerType(ShiftSchedule shiftSchedule, String currentUserId) {
        return shiftSchedule.getOwnerUserId().equals(currentUserId) ? "ME" : "PARTNER";
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

    private CalendarMetaResponse buildMeta(RequestContext context, List<AppUser> members) {
        return new CalendarMetaResponse(
                context.groupId(),
                context.userId(),
                groupQueryService.toCalendarMembers(members),
                DEFAULT_SHIFT_TYPES
        );
    }

    private List<CalendarDaySummaryResponse> buildDaySummaries(
            YearMonth yearMonth,
            List<EventResponse> events,
            List<ShiftResponse> shifts,
            boolean includeShifts
    ) {
        Map<LocalDate, List<ShiftResponse>> shiftsByDate = shifts.stream()
                .collect(Collectors.groupingBy(ShiftResponse::date));
        Map<LocalDate, List<CalendarDayEventResponse>> eventsByDate = buildEventsByDate(yearMonth, events);

        List<CalendarDaySummaryResponse> days = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            List<CalendarDayShiftResponse> dayShifts = shiftsByDate.getOrDefault(date, List.of())
                    .stream()
                    .map(shift -> new CalendarDayShiftResponse(
                            shift.id(),
                            shift.ownerUserId(),
                            shift.ownerType(),
                            shift.shiftType()
                    ))
                    .toList();
            CalendarDayShiftResponse shift = dayShifts.isEmpty() ? null : dayShifts.get(0);
            List<CalendarDayEventResponse> dayEvents = eventsByDate.getOrDefault(date, List.of());

            days.add(new CalendarDaySummaryResponse(
                    date,
                    !includeShifts ? null : shift,
                    includeShifts ? dayShifts : List.of(),
                    dayEvents
            ));
        }
        return days;
    }

    private Map<LocalDate, List<CalendarDayEventResponse>> buildEventsByDate(
            YearMonth yearMonth,
            List<EventResponse> events
    ) {
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        Map<LocalDate, List<CalendarDayEventResponse>> eventsByDate = new java.util.HashMap<>();

        for (EventResponse event : events) {
            LocalDate startDate = event.startDate().isBefore(monthStart) ? monthStart : event.startDate();
            LocalDate endDate = event.endDate().isAfter(monthEnd) ? monthEnd : event.endDate();
            CalendarDayEventResponse dayEvent = new CalendarDayEventResponse(
                    event.id(),
                    event.title(),
                    event.subjectType(),
                    event.ownerUserId(),
                    event.ownerType(),
                    event.startDate(),
                    event.endDate(),
                    event.startTime(),
                    event.endTime(),
                    !event.startDate().equals(event.endDate())
            );

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                eventsByDate.computeIfAbsent(date, ignored -> new ArrayList<>()).add(dayEvent);
            }
        }

        return eventsByDate;
    }

    private Comparator<ShiftResponse> shiftComparator(String currentUserId) {
        return Comparator.comparing((ShiftResponse shift) -> !shift.ownerUserId().equals(currentUserId))
                .thenComparing(ShiftResponse::ownerUserId)
                .thenComparing(ShiftResponse::createdAt);
    }

    private List<ShiftSchedule> findMonthlyShiftsForOwner(
            RequestContext context,
            LocalDate startDate,
            LocalDate endDate,
            String ownerUserId
    ) {
        if (ownerUserId == null) {
            return List.of();
        }

        return shiftScheduleRepository.findActiveShiftsInRangeByOwnerUserId(
                context.groupId(),
                ownerUserId,
                startDate,
                endDate
        );
    }

    private List<ShiftSchedule> findDateShiftForOwner(RequestContext context, LocalDate date, String ownerUserId) {
        if (ownerUserId == null) {
            return List.of();
        }

        return shiftScheduleRepository
                .findByGroupIdAndOwnerUserIdAndDateAndDeletedAtIsNull(context.groupId(), ownerUserId, date)
                .stream()
                .toList();
    }

    private String resolveShiftOwnerUserId(
            RequestContext context,
            List<AppUser> members,
            EventOwnerType shiftOwnerType
    ) {
        if (shiftOwnerType == EventOwnerType.ME) {
            return context.userId();
        }

        return members.stream()
                .map(AppUser::getId)
                .filter(userId -> !userId.equals(context.userId()))
                .findFirst()
                .orElse(null);
    }

    private EventOwnerType resolveEffectiveShiftOwnerType(
            RequestContext context,
            List<AppUser> members,
            EventOwnerType requestedShiftOwnerType
    ) {
        if (requestedShiftOwnerType != null) {
            return requestedShiftOwnerType;
        }

        AppUser currentUser = members.stream()
                .filter(member -> member.getId().equals(context.userId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
        EventOwnerType defaultShiftOwnerType = EventOwnerType.valueOf(currentUser.getDefaultShiftOwnerType().name());
        if (defaultShiftOwnerType == EventOwnerType.PARTNER && !hasPartner(context, members)) {
            return EventOwnerType.ME;
        }
        return defaultShiftOwnerType;
    }

    private boolean hasPartner(RequestContext context, List<AppUser> members) {
        return members.stream()
                .anyMatch(member -> !member.getId().equals(context.userId()));
    }

    private void validateShiftOwnerType(EventOwnerType shiftOwnerType) {
        if (shiftOwnerType == null) {
            return;
        }
        if (shiftOwnerType == EventOwnerType.US) {
            throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "shiftOwnerType은 ME 또는 PARTNER여야 합니다.");
        }
    }

    private boolean matchesOwnerTypes(EventOwnerType ownerType, Set<EventOwnerType> ownerTypes) {
        if (ownerTypes == null || ownerTypes.isEmpty()) {
            return true;
        }

        return ownerTypes.contains(ownerType);
    }

    private Set<EventOwnerType> toAllowedOwnerTypes(List<EventOwnerType> ownerTypes) {
        if (ownerTypes == null || ownerTypes.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(ownerTypes);
    }

    private List<String> toOwnerTypeNames(List<EventOwnerType> ownerTypes) {
        if (ownerTypes == null || ownerTypes.isEmpty()) {
            return DEFAULT_OWNER_TYPES;
        }

        return ownerTypes.stream()
                .map(Enum::name)
                .toList();
    }

}
