package com.schedule.api.group.dto;

import java.util.List;

public record CreateGroupResponse(
        String groupId,
        List<GroupMemberResponse> members
) {
    public static CreateGroupResponse ownerPending(String groupId, String userId) {
        return new CreateGroupResponse(
                groupId,
                List.of(new GroupMemberResponse(userId, "OWNER", "PENDING"))
        );
    }
}
