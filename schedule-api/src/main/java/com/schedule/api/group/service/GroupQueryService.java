package com.schedule.api.group.service;

import com.schedule.api.auth.domain.AppUser;
import com.schedule.api.auth.repository.AppUserRepository;
import com.schedule.api.calendar.dto.CalendarMetaMemberResponse;
import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.exception.ErrorCode;
import com.schedule.api.group.dto.GroupMemberResponse;
import com.schedule.api.group.dto.GroupPermissionsResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GroupQueryService {

    private final AppUserRepository appUserRepository;

    public GroupQueryService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public List<AppUser> loadGroupMembers(String groupId) {
        List<AppUser> members = appUserRepository.findAllByGroupIdOrderByCreatedAtAsc(groupId);
        if (members.isEmpty()) {
            throw new BusinessException(ErrorCode.GROUP_NOT_FOUND);
        }
        return members;
    }

    public List<GroupMemberResponse> toGroupMembers(List<AppUser> members) {
        return members.stream()
                .map(user -> new GroupMemberResponse(
                        user.getId(),
                        isOwner(members, user) ? "OWNER" : "PARTNER",
                        members.size() > 1 ? "CONNECTED" : "PENDING"
                ))
                .toList();
    }

    public List<CalendarMetaMemberResponse> toCalendarMembers(List<AppUser> members) {
        return members.stream()
                .map(user -> new CalendarMetaMemberResponse(
                        user.getId(),
                        isOwner(members, user) ? "OWNER" : "PARTNER"
                ))
                .toList();
    }

    public GroupPermissionsResponse defaultPermissions() {
        return new GroupPermissionsResponse(true, true, true);
    }

    private boolean isOwner(List<AppUser> members, AppUser user) {
        return !members.isEmpty() && members.get(0).getId().equals(user.getId());
    }
}
