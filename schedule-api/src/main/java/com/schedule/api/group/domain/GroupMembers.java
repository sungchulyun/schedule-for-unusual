package com.schedule.api.group.domain;

import com.schedule.api.auth.domain.AppUser;
import com.schedule.api.common.exception.BusinessException;
import com.schedule.api.common.exception.ErrorCode;

import java.util.List;

public class GroupMembers {

    private final List<AppUser> members;

    public GroupMembers(List<AppUser> members) {
        if(members == null || members.isEmpty()){
            throw new BusinessException(ErrorCode.GROUP_PARTNER_ALREADY_EXISTS, "Invite cannot be created when partner already exists");
        }
        this.members = List.copyOf(members);
    }

    public void validateCanRecreateGroup(){
        if(hasPartner()){
            throw new BusinessException(ErrorCode.GROUP_PARTNER_ALREADY_EXISTS, "cannot recreate a group with partner connected");
        }
    }

    public void validateCanRecreateInvite(){
        if(hasPartner()){
            throw new BusinessException(ErrorCode.GROUP_PARTNER_ALREADY_EXISTS, "cannot invite someone with partner connected");
        }
    }

    public void validateCanAcceptInvite(){
        if(hasPartner()){
            throw new BusinessException(ErrorCode.GROUP_MEMBER_LIMIT_EXCEEDED);
        }
    }

    public boolean hasPartner() {
        return members.size() > 1;
    }

    public List<AppUser> values(){
        return members;
    }
}
