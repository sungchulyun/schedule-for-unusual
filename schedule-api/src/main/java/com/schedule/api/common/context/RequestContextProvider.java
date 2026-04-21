package com.schedule.api.common.context;

import com.schedule.api.auth.security.AuthenticatedUser;
import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class RequestContextProvider {

    public RequestContext getRequiredContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED, "Authentication is required");
        }

        if (authenticatedUser.groupId() == null || authenticatedUser.groupId().isBlank()) {
            throw new BusinessException(ErrorCode.GROUP_NOT_FOUND, "Authenticated user group is required");
        }

        return new RequestContext(authenticatedUser.groupId(), authenticatedUser.userId());
    }
}
