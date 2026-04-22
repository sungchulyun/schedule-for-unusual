package com.schedule.api.group.dto;

import java.util.List;

public record AcceptInviteResponse(
        String groupId,
        String inviteId,
        boolean accepted,
        List<GroupMemberResponse> members,
        GroupPermissionsResponse permissions
) {
}
