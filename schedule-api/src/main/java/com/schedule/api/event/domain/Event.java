package com.schedule.api.event.domain;


import com.schedule.api.common.exception.CommonErrorCode;
import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.event.exception.EventErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static com.schedule.api.event.exception.EventErrorCode.EVENT_INVALID_DATE_RANGE;
import static com.schedule.api.event.exception.EventErrorCode.EVENT_NOT_FOUND;

@Entity
@Table(
        name = "events",
        indexes = {
                @Index(name = "idx_events_group_date_deleted", columnList = "group_id,start_date,end_date,deleted_at"),
                @Index(name = "idx_events_group_owner_deleted", columnList = "group_id,owner_user_id,deleted_at")
        }
)
public class Event {

    @Id
    @Column(length = 40, nullable = false)
    private String id;

    @Column(name = "group_id", length = 40, nullable = false)
    private String groupId;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", length = 20, nullable = false)
    private EventSubjectType subjectType;

    @Column(name = "owner_user_id", length = 40)
    private String ownerUserId;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "created_by_user_id", length = 40, nullable = false)
    private String createdByUserId;

    @Column(name = "updated_by_user_id", length = 40, nullable = false)
    private String updatedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Event() {
    }

    private Event(
            String id,
            String groupId,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            EventSubjectType subjectType,
            String ownerUserId,
            String note,
            String createdByUserId,
            String updatedByUserId,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        this.id = id;
        this.groupId = groupId;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.subjectType = subjectType;
        this.ownerUserId = ownerUserId;
        this.note = note;
        this.createdByUserId = createdByUserId;
        this.updatedByUserId = updatedByUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public void update(
            String title,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            EventSubjectType subjectType,
            String ownerUserId,
            String note,
            String updatedByUserId,
            Instant updatedAt
    ) {

        validateUpdatable();

        String normalizedTitle = normalizeTitle(title);
        validateEventPeriod(startDate, endDate, startTime, endTime);
        String normalizedOwnerUserId = normalizeOwnerUserId(subjectType, ownerUserId);

        this.title = normalizedTitle;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.subjectType = subjectType;
        this.ownerUserId = normalizedOwnerUserId;
        this.note = note;
        this.updatedByUserId = updatedByUserId;
        this.updatedAt = updatedAt;
    }

    public void softDelete(String updatedByUserId, Instant deletedAt) {
        if (isDeleted()) {
            return;
        }

        this.updatedByUserId = updatedByUserId;
        this.updatedAt = deletedAt;
        this.deletedAt = deletedAt;
    }

    public static Event create(
            String id,
            String groupId,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            EventSubjectType subjectType,
            String ownerUserId,
            String note,
            String createdByUserId,
            Instant now
    ) {
        String normalizedTitle = normalizeTitle(title);
        validateEventPeriod(startDate, endDate, startTime, endTime);
        String normalizedOwnerUserId = normalizeOwnerUserId(subjectType, ownerUserId);

        return new Event(
                id,
                groupId,
                normalizedTitle,
                startDate,
                endDate,
                startTime,
                endTime,
                subjectType,
                normalizedOwnerUserId,
                note,
                createdByUserId,
                createdByUserId,
                now,
                now,
                null
        );
    }

    private void validateUpdatable(){
        if(isDeleted()){
            throw new BusinessException(EVENT_NOT_FOUND);
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(EventErrorCode.INVALID_EVENT_TITLE);
        }

        return title.trim();
    }

    private static void validateEventPeriod(
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        if (startDate == null || endDate == null || startTime == null || endTime == null) {
            throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
        }

        if(startDate.isAfter(endDate)) {
            throw new BusinessException(EVENT_INVALID_DATE_RANGE);
        }
        if(startDate.equals(endDate) && !startTime.isBefore(endTime)) {
            throw new BusinessException(EVENT_INVALID_DATE_RANGE);
        }
    }

    private static String normalizeOwnerUserId(
            EventSubjectType subjectType,
            String ownerUserId
    ) {
        validateSubjectType(subjectType);

        if (subjectType == EventSubjectType.SHARED) {
            return null;
        }

        validateOwnerRequiredForPersonal(subjectType, ownerUserId);

        return ownerUserId.trim();
    }

    private static void validateSubjectType(EventSubjectType subjectType) {
        if (subjectType == null) {
            throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
        }
    }

    private static void validateOwnerRequiredForPersonal(
            EventSubjectType subjectType,
            String ownerUserId
    ){
        if (subjectType == EventSubjectType.PERSONAL
                && (ownerUserId == null || ownerUserId.isBlank())) {
            throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
        }
    }

    public String getId() {
        return id;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public EventSubjectType getSubjectType() {
        return subjectType;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getNote() {
        return note;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public String getUpdatedByUserId() {
        return updatedByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
