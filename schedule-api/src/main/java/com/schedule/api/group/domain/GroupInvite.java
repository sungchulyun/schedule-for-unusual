package com.schedule.api.group.domain;

import com.schedule.api.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.schedule.api.common.exception.ErrorCode.*;

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

    private GroupInvite(
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

    public static GroupInvite create(
            String id,
            String groupId,
            String code,
            String inviteToken,
            String createdByUserId,
            Instant now
    ){
        return new GroupInvite(
                id,
                groupId,
                code,
                inviteToken,
                InviteStatus.PENDING,
                now.plus(7, ChronoUnit.DAYS),
                createdByUserId,
                now
        );
    }

    public boolean isActive (Instant now){
        return status == InviteStatus.PENDING && expiresAt.isAfter(now);
    }

    public boolean isExpired(Instant now){
        return !expiresAt.isAfter(now);
    }

    public void expireIfExpired(Instant now){
        if(status == InviteStatus.PENDING && isExpired(now)){
            this.status = InviteStatus.EXPIRED;
        }
    }

    public void validateSelfGroupInvite(String userId){
        if(getCreatedByUserId().equals(userId)){
            throw new BusinessException(GROUP_SELF_INVITE_NOT_ALLOWED);
        }
    }

    public boolean isAlreadyAcceptedBy(String userGroupId){
        return userGroupId.equals(groupId) && status == InviteStatus.ACCEPTED;
    }

    public void accept(Instant now){
        if(status != InviteStatus.PENDING) {
            throw new BusinessException(INVALID_GROUP_INVITE_STATUS);
        }

        if(isExpired(now)){
            markExpired();
            throw new BusinessException(GROUP_INVITE_EXPIRED);
        }

        markAccepted();
    }

    private void markAccepted() {
        this.status = InviteStatus.ACCEPTED;
    }

    private void markExpired() {
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
