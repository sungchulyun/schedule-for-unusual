package com.schedule.api.group.service;

import com.schedule.api.auth.domain.AppUser;
import com.schedule.api.auth.repository.AppUserRepository;
import com.schedule.api.auth.security.AuthenticatedUser;
import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.exception.ErrorCode;
import com.schedule.api.common.util.IdGenerator;
import com.schedule.api.group.domain.GroupInvite;
import com.schedule.api.group.domain.InviteStatus;
import com.schedule.api.group.dto.AcceptInviteResponse;
import com.schedule.api.group.dto.CreateGroupResponse;
import com.schedule.api.group.dto.CreateInviteResponse;
import com.schedule.api.group.dto.GroupMeResponse;
import com.schedule.api.group.dto.GroupMemberResponse;
import com.schedule.api.group.dto.InviteInviterResponse;
import com.schedule.api.group.dto.InviteLookupResponse;
import com.schedule.api.group.repository.GroupInviteRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GroupService {
    private static final String SUPPORTED_INVITE_CHANNEL = "KAKAO_TALK_SHARE";

    private final AppUserRepository appUserRepository;
    private final GroupInviteRepository groupInviteRepository;
    private final GroupQueryService groupQueryService;
    private final IdGenerator idGenerator;
    private final String inviteWebBaseUrl;
    private final String inviteDeepLinkBaseUrl;

    public GroupService(
            AppUserRepository appUserRepository,
            GroupInviteRepository groupInviteRepository,
            GroupQueryService groupQueryService,
            IdGenerator idGenerator,
            @Value("${app.group.invite.web-base-url:https://app.example.com/invites}") String inviteWebBaseUrl,
            @Value("${app.group.invite.deep-link-base-url:scheduleapp://invite/accept}") String inviteDeepLinkBaseUrl
    ) {
        this.appUserRepository = appUserRepository;
        this.groupInviteRepository = groupInviteRepository;
        this.groupQueryService = groupQueryService;
        this.idGenerator = idGenerator;
        this.inviteWebBaseUrl = inviteWebBaseUrl;
        this.inviteDeepLinkBaseUrl = inviteDeepLinkBaseUrl;
    }

    public GroupMeResponse getMyGroup(AuthenticatedUser authenticatedUser) {
        List<AppUser> members = groupQueryService.loadGroupMembers(authenticatedUser.groupId());
        return new GroupMeResponse(
                authenticatedUser.groupId(),
                groupQueryService.toGroupMembers(members),
                groupQueryService.defaultPermissions()
        );
    }

    @Transactional
    public CreateGroupResponse createGroup(AuthenticatedUser authenticatedUser) {
        AppUser user = requireUser(authenticatedUser.userId());
        List<AppUser> currentMembers = groupQueryService.loadGroupMembers(user.getGroupId());

        if (currentMembers.size() > 1) {
            throw new BusinessException(ErrorCode.GROUP_PARTNER_ALREADY_EXISTS, "Cannot recreate a group with partner connected");
        }

        String newGroupId = idGenerator.generate("grp_");
        user.changeGroup(newGroupId, Instant.now());

        return new CreateGroupResponse(newGroupId, List.of(new GroupMemberResponse(user.getId(), "OWNER", "PENDING")));
    }

    @Transactional
    public CreateInviteResponse createInvite(AuthenticatedUser authenticatedUser, String channel) {
        validateInviteChannel(channel);

        AppUser user = requireUser(authenticatedUser.userId());
        List<AppUser> members = groupQueryService.loadGroupMembers(user.getGroupId());

        if (members.size() >= 2) {
            throw new BusinessException(ErrorCode.GROUP_PARTNER_ALREADY_EXISTS, "Invite cannot be created when partner already exists");
        }

        groupInviteRepository.findFirstByGroupIdAndStatusOrderByCreatedAtDesc(user.getGroupId(), InviteStatus.PENDING)
                .filter(invite -> invite.getExpiresAt().isAfter(Instant.now()))
                .ifPresent(GroupInvite::markExpired);

        Instant now = Instant.now();
        GroupInvite invite = new GroupInvite(
                idGenerator.generate("inv_"),
                user.getGroupId(),
                generateInviteCode(),
                idGenerator.generate("itk_"),
                InviteStatus.PENDING,
                now.plus(7, ChronoUnit.DAYS),
                user.getId(),
                now
        );

        GroupInvite saved = groupInviteRepository.save(invite);
        return new CreateInviteResponse(
                saved.getId(),
                saved.getGroupId(),
                saved.getCode(),
                saved.getInviteToken(),
                buildShareUrl(saved.getInviteToken()),
                buildDeepLink(saved.getInviteToken()),
                saved.getStatus().name(),
                saved.getExpiresAt()
        );
    }

    @Transactional
    public InviteLookupResponse getInvite(String inviteToken) {
        GroupInvite invite = requireInviteByToken(inviteToken);
        if (invite.getStatus() == InviteStatus.PENDING && invite.getExpiresAt().isBefore(Instant.now())) {
            invite.markExpired();
            throw new BusinessException(ErrorCode.GROUP_INVITE_EXPIRED);
        }

        AppUser inviter = requireUser(invite.getCreatedByUserId());
        return new InviteLookupResponse(
                invite.getId(),
                invite.getGroupId(),
                new InviteInviterResponse(inviter.getId(), inviter.getNickname()),
                invite.getStatus().name(),
                true,
                invite.getExpiresAt()
        );
    }

    @Transactional
    public AcceptInviteResponse acceptInvite(AuthenticatedUser authenticatedUser, String inviteCode, String inviteToken) {
        AppUser user = requireUser(authenticatedUser.userId());
        GroupInvite invite = resolveInvite(inviteCode, inviteToken);

        if (invite.getStatus() == InviteStatus.ACCEPTED && user.getGroupId().equals(invite.getGroupId())) {
            return buildAcceptInviteResponse(invite.getGroupId(), invite.getId());
        }
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new BusinessException(ErrorCode.GROUP_INVITE_NOT_FOUND, "Invite is no longer available");
        }
        if (invite.getExpiresAt().isBefore(Instant.now())) {
            invite.markExpired();
            throw new BusinessException(ErrorCode.GROUP_INVITE_EXPIRED);
        }

        List<AppUser> targetMembers = groupQueryService.loadGroupMembers(invite.getGroupId());
        if (targetMembers.size() >= 2) {
            throw new BusinessException(ErrorCode.GROUP_MEMBER_LIMIT_EXCEEDED);
        }

        List<AppUser> currentMembers = groupQueryService.loadGroupMembers(user.getGroupId());
        if (currentMembers.size() > 1 && !user.getGroupId().equals(invite.getGroupId())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_IN_GROUP);
        }

        user.changeGroup(invite.getGroupId(), Instant.now());
        invite.markAccepted();

        return buildAcceptInviteResponse(invite.getGroupId(), invite.getId());
    }

    public List<GroupMemberResponse> getGroupMembers(String groupId) {
        return groupQueryService.toGroupMembers(groupQueryService.loadGroupMembers(groupId));
    }

    private AppUser requireUser(String userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED, "User not found"));
    }

    private String generateInviteCode() {
        return "CP-" + idGenerator.generate("").replace("_", "").toUpperCase();
    }

    private GroupInvite resolveInvite(String inviteCode, String inviteToken) {
        if (inviteToken != null && !inviteToken.isBlank()) {
            return requireInviteByToken(inviteToken);
        }
        if (inviteCode != null && !inviteCode.isBlank()) {
            return groupInviteRepository.findByCode(inviteCode)
                    .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_INVITE_NOT_FOUND));
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "inviteToken or inviteCode is required");
    }

    private GroupInvite requireInviteByToken(String inviteToken) {
        return groupInviteRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_INVITE_NOT_FOUND));
    }

    private void validateInviteChannel(String channel) {
        if (!SUPPORTED_INVITE_CHANNEL.equals(channel)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "channel must be " + SUPPORTED_INVITE_CHANNEL);
        }
    }

    private AcceptInviteResponse buildAcceptInviteResponse(String groupId, String inviteId) {
        List<AppUser> members = groupQueryService.loadGroupMembers(groupId);
        return new AcceptInviteResponse(
                groupId,
                inviteId,
                true,
                groupQueryService.toGroupMembers(members),
                groupQueryService.defaultPermissions()
        );
    }

    private String buildShareUrl(String inviteToken) {
        return trimTrailingSlash(inviteWebBaseUrl) + "/" + inviteToken;
    }

    private String buildDeepLink(String inviteToken) {
        return trimTrailingSlash(inviteDeepLinkBaseUrl) + "?inviteToken=" + inviteToken;
    }

    private String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
