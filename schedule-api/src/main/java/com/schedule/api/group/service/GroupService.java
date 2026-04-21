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
import com.schedule.api.group.repository.GroupInviteRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GroupService {

    private final AppUserRepository appUserRepository;
    private final GroupInviteRepository groupInviteRepository;
    private final GroupQueryService groupQueryService;
    private final IdGenerator idGenerator;

    public GroupService(
            AppUserRepository appUserRepository,
            GroupInviteRepository groupInviteRepository,
            GroupQueryService groupQueryService,
            IdGenerator idGenerator
    ) {
        this.appUserRepository = appUserRepository;
        this.groupInviteRepository = groupInviteRepository;
        this.groupQueryService = groupQueryService;
        this.idGenerator = idGenerator;
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
    public CreateInviteResponse createInvite(AuthenticatedUser authenticatedUser) {
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
                InviteStatus.PENDING,
                now.plus(7, ChronoUnit.DAYS),
                user.getId(),
                now
        );

        GroupInvite saved = groupInviteRepository.save(invite);
        return new CreateInviteResponse(saved.getGroupId(), saved.getCode(), saved.getExpiresAt());
    }

    @Transactional
    public AcceptInviteResponse acceptInvite(AuthenticatedUser authenticatedUser, String inviteCode) {
        AppUser user = requireUser(authenticatedUser.userId());
        GroupInvite invite = groupInviteRepository.findByCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_INVITE_NOT_FOUND));

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

        List<AppUser> updatedMembers = groupQueryService.loadGroupMembers(invite.getGroupId());
        return new AcceptInviteResponse(
                invite.getGroupId(),
                groupQueryService.toGroupMembers(updatedMembers),
                groupQueryService.defaultPermissions()
        );
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
}
