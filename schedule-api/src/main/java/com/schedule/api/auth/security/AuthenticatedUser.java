package com.schedule.api.auth.security;

import java.io.Serializable;
import java.security.Principal;

public record AuthenticatedUser(
        String userId,
        String groupId,
        String nickname
) implements Principal, Serializable {

    @Override
    public String getName() {
        return userId;
    }
}
