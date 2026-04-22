package com.schedule.api.group.controller;

import com.schedule.api.auth.security.AuthenticatedUser;
import com.schedule.api.common.response.ApiResponse;
import com.schedule.api.group.dto.AcceptInviteRequest;
import com.schedule.api.group.dto.AcceptInviteResponse;
import com.schedule.api.group.dto.CreateGroupResponse;
import com.schedule.api.group.dto.CreateInviteResponse;
import com.schedule.api.group.dto.GroupMeResponse;
import com.schedule.api.group.dto.InviteLookupResponse;
import com.schedule.api.group.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ApiResponse<CreateGroupResponse> createGroup(Authentication authentication) {
        return ApiResponse.success(groupService.createGroup((AuthenticatedUser) authentication.getPrincipal()));
    }

    @GetMapping("/me")
    public ApiResponse<GroupMeResponse> getMyGroup(Authentication authentication) {
        return ApiResponse.success(groupService.getMyGroup((AuthenticatedUser) authentication.getPrincipal()));
    }

    @PostMapping("/partner")
    public ApiResponse<AcceptInviteResponse> connectPartner(
            Authentication authentication,
            @Valid @RequestBody AcceptInviteRequest request
    ) {
        return ApiResponse.success(groupService.acceptInvite(
                (AuthenticatedUser) authentication.getPrincipal(),
                request.inviteCode(),
                request.inviteToken()
        ));
    }

    @PostMapping("/invites")
    public ApiResponse<CreateInviteResponse> createInvite(Authentication authentication) {
        return ApiResponse.success(groupService.createInvite((AuthenticatedUser) authentication.getPrincipal()));
    }

    @GetMapping("/invites/{inviteToken}")
    public ApiResponse<InviteLookupResponse> getInvite(@PathVariable String inviteToken) {
        return ApiResponse.success(groupService.getInvite(inviteToken));
    }

    @PostMapping("/invites/accept")
    public ApiResponse<AcceptInviteResponse> acceptInvite(
            Authentication authentication,
            @Valid @RequestBody AcceptInviteRequest request
    ) {
        return ApiResponse.success(groupService.acceptInvite(
                (AuthenticatedUser) authentication.getPrincipal(),
                request.inviteCode(),
                request.inviteToken()
        ));
    }
}
