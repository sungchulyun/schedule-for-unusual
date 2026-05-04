package com.schedule.api.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private boolean devHeaderEnabled;
    private final Jwt jwt = new Jwt();
    private final Kakao kakao = new Kakao();

    public boolean isDevHeaderEnabled() {
        return devHeaderEnabled;
    }

    public void setDevHeaderEnabled(boolean devHeaderEnabled) {
        this.devHeaderEnabled = devHeaderEnabled;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Kakao getKakao() {
        return kakao;
    }

    public static class Jwt {
        private String issuer = "schedule-api";
        private String secret = "change-me-change-me-change-me-change-me";
        private long accessTokenExpirySeconds = 3600;
        private long refreshTokenExpirySeconds = 7776000;

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessTokenExpirySeconds() {
            return accessTokenExpirySeconds;
        }

        public void setAccessTokenExpirySeconds(long accessTokenExpirySeconds) {
            this.accessTokenExpirySeconds = accessTokenExpirySeconds;
        }

        public long getRefreshTokenExpirySeconds() {
            return refreshTokenExpirySeconds;
        }

        public void setRefreshTokenExpirySeconds(long refreshTokenExpirySeconds) {
            this.refreshTokenExpirySeconds = refreshTokenExpirySeconds;
        }
    }

    public static class Kakao {
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "http://localhost:8080/api/v1/auth/kakao/callback";
        private String authUri = "https://kauth.kakao.com/oauth/authorize";
        private String tokenUri = "https://kauth.kakao.com/oauth/token";
        private String userInfoUri = "https://kapi.kakao.com/v2/user/me";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getAuthUri() {
            return authUri;
        }

        public void setAuthUri(String authUri) {
            this.authUri = authUri;
        }

        public String getTokenUri() {
            return tokenUri;
        }

        public void setTokenUri(String tokenUri) {
            this.tokenUri = tokenUri;
        }

        public String getUserInfoUri() {
            return userInfoUri;
        }

        public void setUserInfoUri(String userInfoUri) {
            this.userInfoUri = userInfoUri;
        }
    }
}
