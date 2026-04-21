package com.schedule.api.shift.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "shift_schedules")
public class ShiftSchedule {

    @Id
    @Column(length = 40, nullable = false)
    private String id;

    @Column(name = "group_id", length = 40, nullable = false)
    private String groupId;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", length = 20, nullable = false)
    private ShiftType shiftType;

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

    protected ShiftSchedule() {
    }

    public ShiftSchedule(
            String id,
            String groupId,
            LocalDate date,
            ShiftType shiftType,
            String createdByUserId,
            String updatedByUserId,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        this.id = id;
        this.groupId = groupId;
        this.date = date;
        this.shiftType = shiftType;
        this.createdByUserId = createdByUserId;
        this.updatedByUserId = updatedByUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public void update(ShiftType shiftType, String updatedByUserId, Instant updatedAt) {
        this.shiftType = shiftType;
        this.updatedByUserId = updatedByUserId;
        this.updatedAt = updatedAt;
        this.deletedAt = null;
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

    public LocalDate getDate() {
        return date;
    }

    public ShiftType getShiftType() {
        return shiftType;
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
