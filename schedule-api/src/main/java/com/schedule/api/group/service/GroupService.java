package com.schedule.api.group.service;

import com.schedule.api.auth.domain.AppUser;
import com.schedule.api.auth.repository.AppUserRepository;
import com.schedule.api.auth.security.AuthenticatedUser;
import com.schedule.api.auth.service.AuthService;
import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.exception.ErrorCode;
import com.schedule.api.common.util.IdGenerator;
import com.schedule.api.group.domain.GroupInvite;
import com.schedule.api.group.domain.GroupMembers;
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
import java.util.Optional;

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
    private final AuthService authService;
    private final IdGenerator idGenerator;
    private final String inviteWebBaseUrl;
    private final String inviteDeepLinkBaseUrl;

    public GroupService(
            AppUserRepository appUserRepository,
            GroupInviteRepository groupInviteRepository,
            GroupQueryService groupQueryService,
            AuthService authService,
            IdGenerator idGenerator,
            @Value("${app.group.invite.web-base-url:https://app.example.com/invites}") String inviteWebBaseUrl,
            @Value("${app.group.invite.deep-link-base-url:scheduleapp://invite/accept}") String inviteDeepLinkBaseUrl
    ) {
        this.appUserRepository = appUserRepository;
        this.groupInviteRepository = groupInviteRepository;
        this.groupQueryService = groupQueryService;
        this.authService = authService;
        this.idGenerator = idGenerator;
        this.inviteWebBaseUrl = inviteWebBaseUrl;
        this.inviteDeepLinkBaseUrl = inviteDeepLinkBaseUrl;
    }

    public GroupMeResponse getMyGroup(AuthenticatedUser authenticatedUser) {
        GroupMembers members = groupQueryService.loadGroupMembers(authenticatedUser.groupId());
        return new GroupMeResponse(
                authenticatedUser.groupId(),
                groupQueryService.toGroupMembers(members.values()),
                groupQueryService.defaultPermissions()
        );
    }

    @Transactional
    public CreateGroupResponse createGroup(AuthenticatedUser authenticatedUser) {
        AppUser user = requireUser(authenticatedUser.userId());

        GroupMembers currentMembers = groupQueryService.loadGroupMembers(user.getGroupId());
        currentMembers.validateCanRecreateGroup();

        String newGroupId = idGenerator.generate("grp_");
        user.recreateGroup(newGroupId, Instant.now());

        return CreateGroupResponse.ownerPending(newGroupId, user.getId());
    }

    @Transactional
    public CreateInviteResponse createInvite(AuthenticatedUser authenticatedUser, String channel) {
        validateInviteChannel(channel);

        AppUser user = requireUser(authenticatedUser.userId());

        GroupMembers members = groupQueryService.loadGroupMembers(user.getGroupId());
        members.validateCanRecreateInvite();

        Instant now = Instant.now();
        Optional<GroupInvite> existingInvite = groupInviteRepository
                .findFirstByGroupIdAndStatusOrderByCreatedAtDesc(user.getGroupId(), InviteStatus.PENDING);

        if(existingInvite.isPresent()){
            GroupInvite invite = existingInvite.get();

            if(invite.isActive(now)){
                return buildCreateInviteResponse(invite);
            }

            invite.expireIfExpired(now);
        }

        GroupInvite invite = GroupInvite.create(
                idGenerator.generate("inv_"),
                user.getGroupId(),
                generateInviteCode(),
                idGenerator.generate("itk_"),
                user.getId(),
                now
        );

        GroupInvite saved = groupInviteRepository.save(invite);
        return buildCreateInviteResponse(saved);
    }

    private CreateInviteResponse buildCreateInviteResponse(GroupInvite saved) {
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
        Instant now = Instant.now();

        if(!invite.isActive(now)){
            invite.expireIfExpired(now);
            throw new BusinessException(ErrorCode.GROUP_INVITE_EXPIRED);
        }

        AppUser inviter = requireUser(invite.getCreatedByUserId());
        return buildInviteLookupResponse(invite, inviter);
    }

    private InviteLookupResponse buildInviteLookupResponse(GroupInvite invite, AppUser inviter) {
        return new InviteLookupResponse(
                invite.getId(),
                invite.getGroupId(),
                new InviteInviterResponse(
                        inviter.getId(),
                        inviter.getNickname()
                ),
                invite.getStatus().name(),
                true,
                invite.getExpiresAt()
        );
    }

    @Transactional
    public AcceptInviteResponse acceptInvite(AuthenticatedUser authenticatedUser, String inviteCode, String inviteToken) {
        Instant now = Instant.now();

        AppUser user = requireUser(authenticatedUser.userId());

        GroupInvite invite = resolveInvite(inviteCode, inviteToken);
        invite.validateSelfGroupInvite(user.getId());

        if (invite.isAlreadyAcceptedBy(user.getGroupId())) {
            return buildAcceptInviteResponse(invite.getGroupId(), invite.getId(), user);
        }

        GroupMembers targetMembers = groupQueryService.loadGroupMembers(invite.getGroupId());
        targetMembers.validateCanAcceptInvite();

        GroupMembers currentMembers = groupQueryService.loadGroupMembers(user.getGroupId());
        currentMembers.validateCanAcceptInvite();

        user.joinGroup(invite.getGroupId(), now);

        invite.accept(now);

        return buildAcceptInviteResponse(invite.getGroupId(), invite.getId(), user);
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

    private AcceptInviteResponse buildAcceptInviteResponse(String groupId, String inviteId, AppUser user) {
        GroupMembers members = groupQueryService.loadGroupMembers(groupId);
        return new AcceptInviteResponse(
                groupId,
                inviteId,
                true,
                groupQueryService.toGroupMembers(members.values()),
                groupQueryService.defaultPermissions(),
                authService.issueTokensForUser(user)
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
