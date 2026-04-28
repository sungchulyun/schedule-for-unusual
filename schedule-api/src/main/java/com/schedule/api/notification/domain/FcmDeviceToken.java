package com.schedule.api.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "fcm_device_tokens")
public class FcmDeviceToken {

    @Id
    @Column(length = 512, nullable = false)
    private String token;

    @Column(name = "user_id", length = 40, nullable = false)
    private String userId;

    @Column(name = "group_id", length = 40, nullable = false)
    private String groupId;

    @Column(length = 30)
    private String platform;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FcmDeviceToken() {
    }

    public FcmDeviceToken(String token, String userId, String groupId, String platform, Instant now) {
        this.token = token;
        this.userId = userId;
        this.groupId = groupId;
        this.platform = platform;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateOwner(String userId, String groupId, String platform, Instant updatedAt) {
        this.userId = userId;
        this.groupId = groupId;
        this.platform = platform;
        this.updatedAt = updatedAt;
    }

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public String getGroupId() {
        return groupId;
    }
}
