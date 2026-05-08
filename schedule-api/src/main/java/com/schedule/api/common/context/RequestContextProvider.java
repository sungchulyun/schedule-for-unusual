package com.schedule.api.common.context;


import com.schedule.api.auth.exception.AuthErrorCode;
import com.schedule.api.group.exception.GroupErrorCode;
import com.schedule.api.auth.security.AuthenticatedUser;
import com.schedule.api.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class RequestContextProvider {

    public RequestContext getRequiredContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new BusinessException(AuthErrorCode.AUTH_UNAUTHORIZED, "인증이 필요합니다.");
        }

        if (authenticatedUser.groupId() == null || authenticatedUser.groupId().isBlank()) {
            throw new BusinessException(GroupErrorCode.GROUP_NOT_FOUND, "인증된 사용자 그룹이 필요합니다.");
        }

        return new RequestContext(authenticatedUser.groupId(), authenticatedUser.userId());
    }
}
