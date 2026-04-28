package com.schedule.api.group.dto;

import com.schedule.api.auth.dto.AuthTokenResponse;
import java.util.List;

public record AcceptInviteResponse(
        String groupId,
        String inviteId,
        boolean accepted,
        List<GroupMemberResponse> members,
        GroupPermissionsResponse permissions,
        AuthTokenResponse tokens
) {
}
