package com.schedule.api.auth.security;

import com.schedule.api.auth.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final AuthProperties authProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.secretKey = Keys.hmacShaKeyFor(authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(authProperties.getJwt().getAccessTokenExpirySeconds());

        return Jwts.builder()
                .issuer(authProperties.getJwt().getIssuer())
                .subject(user.userId())
                .claim("groupId", user.groupId())
                .claim("nickname", user.nickname())
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public RefreshTokenPayload createRefreshToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(authProperties.getJwt().getRefreshTokenExpirySeconds());
        String tokenKey = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .issuer(authProperties.getJwt().getIssuer())
                .subject(user.userId())
                .claim("groupId", user.groupId())
                .claim("nickname", user.nickname())
                .claim("type", "refresh")
                .claim("tokenKey", tokenKey)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();

        return new RefreshTokenPayload(token, tokenKey, expiresAt);
    }

    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims = parseClaims(token);
        return new AuthenticatedUser(
                claims.getSubject(),
                claims.get("groupId", String.class),
                claims.get("nickname", String.class)
        );
    }

    public RefreshClaims parseRefreshToken(String token) {
        Claims claims = parseClaims(token);
        return new RefreshClaims(
                claims.getSubject(),
                claims.get("groupId", String.class),
                claims.get("nickname", String.class),
                claims.get("tokenKey", String.class),
                claims.getExpiration().toInstant()
        );
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpirySeconds() {
        return authProperties.getJwt().getAccessTokenExpirySeconds();
    }

    public long getRefreshTokenExpirySeconds() {
        return authProperties.getJwt().getRefreshTokenExpirySeconds();
    }

    public record RefreshTokenPayload(
            String token,
            String tokenKey,
            Instant expiresAt
    ) {
    }

    public record RefreshClaims(
            String userId,
            String groupId,
            String nickname,
            String tokenKey,
            Instant expiresAt
    ) {
    }
}
