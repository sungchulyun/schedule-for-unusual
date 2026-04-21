package com.schedule.api.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @Column(length = 40, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", length = 20, nullable = false)
    private OAuthProvider oauthProvider;

    @Column(name = "oauth_provider_user_id", length = 100, nullable = false, unique = true)
    private String oauthProviderUserId;

    @Column(length = 50, nullable = false)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "group_id", length = 40, nullable = false)
    private String groupId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private UserStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppUser() {
    }

    public AppUser(
            String id,
            OAuthProvider oauthProvider,
            String oauthProviderUserId,
            String nickname,
            String profileImageUrl,
            String groupId,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.oauthProvider = oauthProvider;
        this.oauthProviderUserId = oauthProviderUserId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.groupId = groupId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateProfile(String nickname, String profileImageUrl, Instant updatedAt) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.updatedAt = updatedAt;
    }

    public void changeGroup(String groupId, Instant updatedAt) {
        this.groupId = groupId;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public OAuthProvider getOauthProvider() {
        return oauthProvider;
    }

    public String getOauthProviderUserId() {
        return oauthProviderUserId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getGroupId() {
        return groupId;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
