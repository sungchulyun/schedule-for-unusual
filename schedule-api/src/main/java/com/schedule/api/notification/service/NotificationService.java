package com.schedule.api.notification.service;

import com.schedule.api.auth.domain.AppUser;
import com.schedule.api.auth.repository.AppUserRepository;
import com.schedule.api.common.context.RequestContext;
import com.schedule.api.event.domain.Event;
import com.schedule.api.event.domain.EventSubjectType;
import com.schedule.api.event.repository.EventRepository;
import com.schedule.api.notification.config.FcmProperties;
import com.schedule.api.notification.domain.FcmDeviceToken;
import com.schedule.api.notification.dto.FcmTokenRequest;
import com.schedule.api.notification.dto.FcmTokenResponse;
import com.schedule.api.notification.event.ScheduleChangeType;
import com.schedule.api.notification.event.ScheduleChangedEvent;
import com.schedule.api.notification.repository.FcmDeviceTokenRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Service
public class NotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M월 d일");

    private final FcmDeviceTokenRepository tokenRepository;
    private final AppUserRepository appUserRepository;
    private final EventRepository eventRepository;
    private final FcmMessageSender messageSender;
    private final FcmProperties fcmProperties;

    public NotificationService(
            FcmDeviceTokenRepository tokenRepository,
            AppUserRepository appUserRepository,
            EventRepository eventRepository,
            FcmMessageSender messageSender,
            FcmProperties fcmProperties
    ) {
        this.tokenRepository = tokenRepository;
        this.appUserRepository = appUserRepository;
        this.eventRepository = eventRepository;
        this.messageSender = messageSender;
        this.fcmProperties = fcmProperties;
    }

    @Transactional
    public FcmTokenResponse registerToken(RequestContext context, FcmTokenRequest request) {
        String token = request.token().trim();
        String platform = request.platform() == null ? null : request.platform().trim();
        Instant now = Instant.now();
        FcmDeviceToken deviceToken = tokenRepository.findById(token)
                .map(existing -> {
                    existing.updateOwner(context.userId(), context.groupId(), platform, now);
                    return existing;
                })
                .orElseGet(() -> new FcmDeviceToken(token, context.userId(), context.groupId(), platform, now));
        tokenRepository.save(deviceToken);
        return new FcmTokenResponse(true);
    }

    @Transactional
    public void unregisterToken(RequestContext context, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        tokenRepository.findById(token.trim())
                .filter(deviceToken -> deviceToken.getUserId().equals(context.userId()))
                .ifPresent(tokenRepository::delete);
    }

    public ScheduleChangedEvent toScheduleChangedEvent(
            Event event,
            String actorUserId,
            ScheduleChangeType changeType
    ) {
        return new ScheduleChangedEvent(
                event.getGroupId(),
                actorUserId,
                event.getTitle(),
                event.getStartDate(),
                event.getEndDate(),
                changeType
        );
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onScheduleChanged(ScheduleChangedEvent event) {
        List<String> partnerUserIds = appUserRepository.findAllByGroupIdOrderByCreatedAtAsc(event.groupId())
                .stream()
                .map(AppUser::getId)
                .filter(userId -> !userId.equals(event.actorUserId()))
                .toList();
        List<String> tokens = tokenRepository.findAllByUserIdIn(partnerUserIds)
                .stream()
                .map(FcmDeviceToken::getToken)
                .toList();

        messageSender.send(
                tokens,
                "일정이 " + changeLabel(event.changeType()) + "됐어요",
                event.title() + " · " + formatDateRange(event.startDate(), event.endDate()),
                Map.of("type", "SCHEDULE_CHANGED", "changeType", event.changeType().name())
        );
    }

    @Scheduled(cron = "${app.fcm.daily-summary-cron:0 0 8 * * *}", zone = "${app.fcm.zone-id:Asia/Seoul}")
    public void sendDailySummary() {
        LocalDate today = LocalDate.now(ZoneId.of(fcmProperties.getZoneId()));
        appUserRepository.findAll()
                .stream()
                .map(AppUser::getGroupId)
                .distinct()
                .forEach(groupId -> sendDailySummaryForGroup(groupId, today));
    }

    private void sendDailySummaryForGroup(String groupId, LocalDate today) {
        List<AppUser> members = appUserRepository.findAllByGroupIdOrderByCreatedAtAsc(groupId);
        for (AppUser recipient : members) {
            DailyScheduleCounts counts = countDailySchedules(groupId, members, recipient.getId(), today);
            List<String> tokens = tokenRepository.findAllByUserIdIn(List.of(recipient.getId()))
                    .stream()
                    .map(FcmDeviceToken::getToken)
                    .toList();
            messageSender.send(
                    tokens,
                    "오늘 일정",
                    "내 일정 : " + counts.mine() + ", 상대 일정 : " + counts.partner() + ", 우리 일정 : " + counts.shared(),
                    Map.of("type", "DAILY_SCHEDULE_SUMMARY", "date", today.toString())
            );
        }
    }

    private DailyScheduleCounts countDailySchedules(
            String groupId,
            List<AppUser> members,
            String recipientUserId,
            LocalDate today
    ) {
        List<String> memberUserIds = members.stream().map(AppUser::getId).toList();
        List<String> partnerUserIds = memberUserIds.stream()
                .filter(userId -> !userId.equals(recipientUserId))
                .toList();

        int mine = 0;
        int partner = 0;
        int shared = 0;
        for (Event event : eventRepository.findActiveEventsInRange(groupId, today, today)) {
            if (event.getSubjectType() == EventSubjectType.SHARED) {
                shared++;
            } else if (recipientUserId.equals(event.getOwnerUserId())) {
                mine++;
            } else if (partnerUserIds.contains(event.getOwnerUserId())) {
                partner++;
            }
        }
        return new DailyScheduleCounts(mine, partner, shared);
    }

    private String changeLabel(ScheduleChangeType changeType) {
        return switch (changeType) {
            case CREATED -> "등록";
            case UPDATED -> "수정";
            case DELETED -> "삭제";
        };
    }

    private String formatDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.equals(endDate)) {
            return startDate.format(DATE_FORMATTER);
        }
        return startDate.format(DATE_FORMATTER) + " - " + endDate.format(DATE_FORMATTER);
    }

    private record DailyScheduleCounts(int mine, int partner, int shared) {
    }
}
