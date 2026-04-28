package com.schedule.api.notification.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.schedule.api.notification.config.FcmProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class FcmMessageSender {

    private static final Logger log = LoggerFactory.getLogger(FcmMessageSender.class);
    private static final String APP_NAME = "schedule-api";

    private final FcmProperties properties;
    private final ResourceLoader resourceLoader;
    private FirebaseMessaging firebaseMessaging;
    private boolean initializationAttempted;

    public FcmMessageSender(FcmProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    public void send(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        FirebaseMessaging messaging = resolveMessaging().orElse(null);
        if (messaging == null) {
            return;
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data == null ? Map.of() : data)
                .build();

        try {
            BatchResponse response = messaging.sendEachForMulticast(message);
            if (response.getFailureCount() > 0) {
                log.warn("FCM send completed with failures. success={}, failure={}",
                        response.getSuccessCount(),
                        response.getFailureCount());
            }
        } catch (FirebaseMessagingException e) {
            log.warn("FCM send failed", e);
        }
    }

    private synchronized Optional<FirebaseMessaging> resolveMessaging() {
        if (firebaseMessaging != null) {
            return Optional.of(firebaseMessaging);
        }
        if (initializationAttempted || !properties.isEnabled()) {
            return Optional.empty();
        }

        initializationAttempted = true;
        String credentialsPath = configuredCredentialsPath();
        if (credentialsPath == null) {
            log.warn("FCM is enabled but credentials path is empty. Set app.fcm.credentials-path or GOOGLE_APPLICATION_CREDENTIALS.");
            return Optional.empty();
        }

        try (InputStream credentials = openCredentials(credentialsPath)) {
            FirebaseApp app = FirebaseApp.getApps().stream()
                    .filter(candidate -> APP_NAME.equals(candidate.getName()))
                    .findFirst()
                    .orElseGet(() -> initializeApp(credentials));
            firebaseMessaging = FirebaseMessaging.getInstance(app);
            return Optional.of(firebaseMessaging);
        } catch (IOException e) {
            log.warn("FCM credentials could not be loaded: {}", credentialsPath, e);
            return Optional.empty();
        }
    }

    private FirebaseApp initializeApp(InputStream credentials) {
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();
            return FirebaseApp.initializeApp(options, APP_NAME);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize Firebase", e);
        }
    }

    private InputStream openCredentials(String credentialsPath) throws IOException {
        if (credentialsPath.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX)) {
            Resource resource = resourceLoader.getResource(credentialsPath);
            if (!resource.exists()) {
                throw new IOException("classpath resource not found");
            }
            return resource.getInputStream();
        }
        return Files.newInputStream(Path.of(credentialsPath));
    }

    private String configuredCredentialsPath() {
        String configured = properties.getCredentialsPath();
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        String env = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        return env == null || env.isBlank() ? null : env.trim();
    }
}
