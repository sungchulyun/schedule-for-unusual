package com.schedule.api.common.context;

public record RequestContext(
        String groupId,
        String userId
) {
}
