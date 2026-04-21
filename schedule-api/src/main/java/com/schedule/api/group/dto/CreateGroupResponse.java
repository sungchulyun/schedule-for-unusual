package com.schedule.api.group.dto;

import java.util.List;

public record CreateGroupResponse(
        String groupId,
        List<GroupMemberResponse> members
) {
}
