package com.schedule.api.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @Column(length = 40, nullable = false)
    private String id;

    @Column(name = "user_id", length = 40, nullable = false)
    private String userId;

    @Column(name = "token_key", length = 100, nullable = false, unique = true)
    private String tokenKey;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshToken() {
    }

    public RefreshToken(String id, String userId, String tokenKey, Instant expiresAt, Instant revokedAt, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenKey = tokenKey;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.createdAt = createdAt;
    }

    public void revoke(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getTokenKey() {
        return tokenKey;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String isRevoked() {
        return revokedAt == null ? null : revokedAt.toString();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
