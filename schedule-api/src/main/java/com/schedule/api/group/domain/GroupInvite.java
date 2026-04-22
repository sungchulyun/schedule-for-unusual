package com.schedule.api.group.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "group_invites")
public class GroupInvite {

    @Id
    @Column(length = 40, nullable = false)
    private String id;

    @Column(name = "group_id", length = 40, nullable = false)
    private String groupId;

    @Column(length = 40, nullable = false, unique = true)
    private String code;

    @Column(name = "invite_token", length = 60, nullable = false, unique = true)
    private String inviteToken;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private InviteStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_by_user_id", length = 40, nullable = false)
    private String createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GroupInvite() {
    }

    public GroupInvite(
            String id,
            String groupId,
            String code,
            String inviteToken,
            InviteStatus status,
            Instant expiresAt,
            String createdByUserId,
            Instant createdAt
    ) {
        this.id = id;
        this.groupId = groupId;
        this.code = code;
        this.inviteToken = inviteToken;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
    }

    public void markAccepted() {
        this.status = InviteStatus.ACCEPTED;
    }

    public void markExpired() {
        this.status = InviteStatus.EXPIRED;
    }

    public String getId() {
        return id;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getCode() {
        return code;
    }

    public String getInviteToken() {
        return inviteToken;
    }

    public InviteStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
