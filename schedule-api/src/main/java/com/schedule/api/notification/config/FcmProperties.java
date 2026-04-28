package com.schedule.api.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.fcm")
public class FcmProperties {

    private boolean enabled = false;
    private String credentialsPath;
    private String dailySummaryCron = "0 0 8 * * *";
    private String zoneId = "Asia/Seoul";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCredentialsPath() {
        return credentialsPath;
    }

    public void setCredentialsPath(String credentialsPath) {
        this.credentialsPath = credentialsPath;
    }

    public String getDailySummaryCron() {
        return dailySummaryCron;
    }

    public void setDailySummaryCron(String dailySummaryCron) {
        this.dailySummaryCron = dailySummaryCron;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }
}
