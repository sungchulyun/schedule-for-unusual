package com.schedule.api.event.service;

import com.schedule.api.auth.domain.AppUser;
import com.schedule.api.auth.repository.AppUserRepository;
import com.schedule.api.common.context.RequestContext;
import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.exception.ErrorCode;
import com.schedule.api.common.util.IdGenerator;
import com.schedule.api.common.util.YearMonthValidator;
import com.schedule.api.event.dto.CreateEventRequest;
import com.schedule.api.event.dto.DeleteEventResponse;
import com.schedule.api.event.dto.EventDateResponse;
import com.schedule.api.event.dto.EventMonthResponse;
import com.schedule.api.event.dto.EventResponse;
import com.schedule.api.event.dto.UpdateEventRequest;
import com.schedule.api.event.domain.Event;
import com.schedule.api.event.domain.EventOwnerType;
import com.schedule.api.event.domain.EventSubjectType;
import com.schedule.api.event.repository.EventRepository;
import com.schedule.api.notification.event.ScheduleChangeType;
import com.schedule.api.notification.service.NotificationService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final AppUserRepository appUserRepository;
    private final IdGenerator idGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    public EventService(
            EventRepository eventRepository,
            AppUserRepository appUserRepository,
            IdGenerator idGenerator,
            ApplicationEventPublisher eventPublisher,
            NotificationService notificationService
    ) {
        this.eventRepository = eventRepository;
        this.appUserRepository = appUserRepository;
        this.idGenerator = idGenerator;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
    }

    public EventMonthResponse getMonthlyEvents(
            RequestContext context,
            int year,
            int month,
            List<EventOwnerType> ownerTypes
    ) {
        YearMonthValidator.validate(year, month);
        YearMonth yearMonth = YearMonth.of(year, month);

        List<EventResponse> items = eventRepository.findActiveEventsInRange(
                        context.groupId(),
                        yearMonth.atDay(1),
                        yearMonth.atEndOfMonth()
                )
                .stream()
                .map(event -> toResponse(event, context.userId()))
                .filter(event -> matchesOwnerTypes(event, ownerTypes))
                .toList();

        return new EventMonthResponse(year, month, items);
    }

    public EventDateResponse getDateEvents(RequestContext context, LocalDate date, List<EventOwnerType> ownerTypes) {
        List<EventResponse> items = eventRepository.findActiveEventsInRange(context.groupId(), date, date)
                .stream()
                .map(event -> toResponse(event, context.userId()))
                .filter(event -> matchesOwnerTypes(event, ownerTypes))
                .toList();

        return new EventDateResponse(date, items);
    }

    @Transactional
    public EventResponse createEvent(RequestContext context, CreateEventRequest request) {
        validateTitle(request.title());
        validateEventRule(
                request.startDate(),
                request.endDate(),
                request.startTime(),
                request.endTime(),
                request.subjectType(),
                request.ownerUserId()
        );
        validateGroupMembership(context, request.subjectType(), request.ownerUserId());

        Instant now = Instant.now();
        Event event = new Event(
                idGenerator.generate("evt_"),
                context.groupId(),
                request.title().trim(),
                request.startDate(),
                request.endDate(),
                request.startTime(),
                request.endTime(),
                request.subjectType(),
                normalizedOwnerUserId(request.subjectType(), request.ownerUserId()),
                request.note(),
                context.userId(),
                context.userId(),
                now,
                now,
                null
        );

        Event savedEvent = eventRepository.save(event);
        publishScheduleChanged(savedEvent, context.userId(), ScheduleChangeType.CREATED);
        return toResponse(savedEvent, context.userId());
    }

    @Transactional
    public EventResponse updateEvent(RequestContext context, String eventId, UpdateEventRequest request) {
        Event event = eventRepository.findByIdAndGroupIdAndDeletedAtIsNull(eventId, context.groupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        String title = request.title() != null ? request.title().trim() : event.getTitle();
        LocalDate startDate = request.startDate() != null ? request.startDate() : event.getStartDate();
        LocalDate endDate = request.endDate() != null ? request.endDate() : event.getEndDate();
        LocalTime startTime = request.startTime() != null ? request.startTime() : event.getStartTime();
        LocalTime endTime = request.endTime() != null ? request.endTime() : event.getEndTime();
        EventSubjectType subjectType = request.subjectType() != null ? request.subjectType() : event.getSubjectType();
        String ownerUserId = request.ownerUserId() != null || subjectType == EventSubjectType.SHARED
                ? request.ownerUserId()
                : event.getOwnerUserId();
        String note = request.note() != null ? request.note() : event.getNote();

        validateTitle(title);
        validateEventRule(startDate, endDate, startTime, endTime, subjectType, ownerUserId);
        validateGroupMembership(context, subjectType, ownerUserId);

        event.update(
                title,
                startDate,
                endDate,
                startTime,
                endTime,
                subjectType,
                normalizedOwnerUserId(subjectType, ownerUserId),
                note,
                context.userId(),
                Instant.now()
        );

        publishScheduleChanged(event, context.userId(), ScheduleChangeType.UPDATED);
        return toResponse(event, context.userId());
    }

    @Transactional
    public DeleteEventResponse deleteEvent(RequestContext context, String eventId) {
        Event event = eventRepository.findByIdAndGroupIdAndDeletedAtIsNull(eventId, context.groupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        Instant deletedAt = Instant.now();
        event.softDelete(context.userId(), deletedAt);

        publishScheduleChanged(event, context.userId(), ScheduleChangeType.DELETED);
        return new DeleteEventResponse(event.getId(), true, deletedAt);
    }

    private void publishScheduleChanged(Event event, String actorUserId, ScheduleChangeType changeType) {
        eventPublisher.publishEvent(notificationService.toScheduleChangedEvent(event, actorUserId, changeType));
    }

    private void validateEventRule(
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            EventSubjectType subjectType,
            String ownerUserId
    ) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_DATE_RANGE, "startDate must be before or equal to endDate");
        }

        if (startDate.equals(endDate) && startTime.isAfter(endTime)) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_DATE_RANGE, "startTime must be before or equal to endTime on the same date");
        }

        if (subjectType == EventSubjectType.PERSONAL && (ownerUserId == null || ownerUserId.isBlank())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ownerUserId is required when subjectType is PERSONAL");
        }
    }

    private void validateGroupMembership(RequestContext context, EventSubjectType subjectType, String ownerUserId) {
        List<AppUser> members = appUserRepository.findAllByGroupIdOrderByCreatedAtAsc(context.groupId());
        if (members.isEmpty()) {
            return;
        }

        boolean currentUserInGroup = members.stream()
                .anyMatch(member -> member.getId().equals(context.userId()));
        if (!currentUserInGroup) {
            throw new BusinessException(ErrorCode.GROUP_ACCESS_DENIED);
        }

        if (subjectType != EventSubjectType.PERSONAL) {
            return;
        }

        String normalizedOwnerUserId = ownerUserId == null ? null : ownerUserId.trim();
        boolean ownerInGroup = members.stream()
                .anyMatch(member -> member.getId().equals(normalizedOwnerUserId));
        if (!ownerInGroup) {
            throw new BusinessException(
                    ErrorCode.GROUP_ACCESS_DENIED,
                    "ownerUserId must be a member of the current group"
            );
        }
    }

    private String normalizedOwnerUserId(EventSubjectType subjectType, String ownerUserId) {
        if (subjectType == EventSubjectType.SHARED) {
            return null;
        }

        return ownerUserId == null ? null : ownerUserId.trim();
    }

    private boolean matchesOwnerTypes(EventResponse event, List<EventOwnerType> ownerTypes) {
        if (ownerTypes == null || ownerTypes.isEmpty()) {
            return true;
        }

        Set<EventOwnerType> allowedTypes = Set.copyOf(ownerTypes);
        return allowedTypes.contains(event.ownerType());
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "title must not be blank");
        }
    }

    private EventResponse toResponse(Event event, String currentUserId) {
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

    private EventOwnerType resolveOwnerType(Event event, String currentUserId) {
        if (event.getSubjectType() == EventSubjectType.SHARED) {
            return EventOwnerType.US;
        }

        if (event.getOwnerUserId() != null && event.getOwnerUserId().equals(currentUserId)) {
            return EventOwnerType.ME;
        }

        return EventOwnerType.PARTNER;
    }
}
