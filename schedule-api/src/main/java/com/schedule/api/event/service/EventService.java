package com.schedule.api.event.service;

import com.schedule.api.common.context.RequestContext;
import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.exception.ErrorCode;
import com.schedule.api.common.util.IdGenerator;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final IdGenerator idGenerator;

    public EventService(EventRepository eventRepository, IdGenerator idGenerator) {
        this.eventRepository = eventRepository;
        this.idGenerator = idGenerator;
    }

    public EventMonthResponse getMonthlyEvents(RequestContext context, int year, int month) {
        validateYearMonth(year, month);
        YearMonth yearMonth = YearMonth.of(year, month);

        List<EventResponse> items = eventRepository.findActiveEventsInRange(
                        context.groupId(),
                        yearMonth.atDay(1),
                        yearMonth.atEndOfMonth()
                )
                .stream()
                .map(event -> toResponse(event, context.userId()))
                .toList();

        return new EventMonthResponse(year, month, items);
    }

    public EventDateResponse getDateEvents(RequestContext context, LocalDate date) {
        List<EventResponse> items = eventRepository.findActiveEventsInRange(context.groupId(), date, date)
                .stream()
                .map(event -> toResponse(event, context.userId()))
                .toList();

        return new EventDateResponse(date, items);
    }

    @Transactional
    public EventResponse createEvent(RequestContext context, CreateEventRequest request) {
        validateEventRule(request.startDate(), request.endDate(), request.subjectType(), request.ownerUserId());

        Instant now = Instant.now();
        Event event = new Event(
                idGenerator.generate("evt_"),
                context.groupId(),
                request.title().trim(),
                request.startDate(),
                request.endDate(),
                request.subjectType(),
                normalizedOwnerUserId(request.subjectType(), request.ownerUserId()),
                request.note(),
                context.userId(),
                context.userId(),
                now,
                now,
                null
        );

        return toResponse(eventRepository.save(event), context.userId());
    }

    @Transactional
    public EventResponse updateEvent(RequestContext context, String eventId, UpdateEventRequest request) {
        Event event = eventRepository.findByIdAndGroupIdAndDeletedAtIsNull(eventId, context.groupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        String title = request.title() != null ? request.title().trim() : event.getTitle();
        LocalDate startDate = request.startDate() != null ? request.startDate() : event.getStartDate();
        LocalDate endDate = request.endDate() != null ? request.endDate() : event.getEndDate();
        EventSubjectType subjectType = request.subjectType() != null ? request.subjectType() : event.getSubjectType();
        String ownerUserId = request.ownerUserId() != null || subjectType == EventSubjectType.SHARED
                ? request.ownerUserId()
                : event.getOwnerUserId();
        String note = request.note() != null ? request.note() : event.getNote();

        validateEventRule(startDate, endDate, subjectType, ownerUserId);

        event.update(
                title,
                startDate,
                endDate,
                subjectType,
                normalizedOwnerUserId(subjectType, ownerUserId),
                note,
                context.userId(),
                Instant.now()
        );

        return toResponse(event, context.userId());
    }

    @Transactional
    public DeleteEventResponse deleteEvent(RequestContext context, String eventId) {
        Event event = eventRepository.findByIdAndGroupIdAndDeletedAtIsNull(eventId, context.groupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        Instant deletedAt = Instant.now();
        event.softDelete(context.userId(), deletedAt);

        return new DeleteEventResponse(event.getId(), true, deletedAt);
    }

    private void validateYearMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "month must be between 1 and 12");
        }

        if (year < 2000 || year > 2100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "year must be between 2000 and 2100");
        }
    }

    private void validateEventRule(
            LocalDate startDate,
            LocalDate endDate,
            EventSubjectType subjectType,
            String ownerUserId
    ) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_DATE_RANGE, "startDate must be before or equal to endDate");
        }

        if (subjectType == EventSubjectType.PERSONAL && (ownerUserId == null || ownerUserId.isBlank())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ownerUserId is required when subjectType is PERSONAL");
        }
    }

    private String normalizedOwnerUserId(EventSubjectType subjectType, String ownerUserId) {
        if (subjectType == EventSubjectType.SHARED) {
            return null;
        }

        return ownerUserId == null ? null : ownerUserId.trim();
    }

    private EventResponse toResponse(Event event, String currentUserId) {
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
