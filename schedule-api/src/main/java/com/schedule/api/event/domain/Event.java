package com.schedule.api.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "events")
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

    public Event(
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
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.subjectType = subjectType;
        this.ownerUserId = ownerUserId;
        this.note = note;
        this.updatedByUserId = updatedByUserId;
        this.updatedAt = updatedAt;
    }

    public void softDelete(String updatedByUserId, Instant deletedAt) {
        this.updatedByUserId = updatedByUserId;
        this.updatedAt = deletedAt;
        this.deletedAt = deletedAt;
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
