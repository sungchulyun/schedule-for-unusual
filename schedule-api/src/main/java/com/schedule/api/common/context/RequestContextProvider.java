package com.schedule.api.common.context;

import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class RequestContextProvider {

    private static final String GROUP_ID_HEADER = "X-Group-Id";
    private static final String USER_ID_HEADER = "X-User-Id";

    public RequestContext getRequiredContext() {
        HttpServletRequest request = currentRequest();

        String groupId = request.getHeader(GROUP_ID_HEADER);
        String userId = request.getHeader(USER_ID_HEADER);

        if (groupId == null || groupId.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, GROUP_ID_HEADER + " header is required");
        }

        if (userId == null || userId.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, USER_ID_HEADER + " header is required");
        }

        return new RequestContext(groupId, userId);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Request context is not available");
        }

        return attributes.getRequest();
    }
}
