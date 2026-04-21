package com.schedule.api.group.dto;

import java.util.List;

public record AcceptInviteResponse(
        String groupId,
        List<GroupMemberResponse> members,
        GroupPermissionsResponse permissions
) {
}
