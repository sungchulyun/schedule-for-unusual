package com.schedule.api.group.dto;

import java.util.List;

public record GroupMeResponse(
        String groupId,
        List<GroupMemberResponse> members,
        GroupPermissionsResponse permissions
) {
}
