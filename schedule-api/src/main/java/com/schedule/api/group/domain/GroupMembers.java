package com.schedule.api.group.domain;


import com.schedule.api.group.exception.GroupErrorCode;
import com.schedule.api.auth.domain.AppUser;
import com.schedule.api.common.exception.BusinessException;

import java.util.List;

public class GroupMembers {

    private final List<AppUser> members;

    public GroupMembers(List<AppUser> members) {
        if(members == null || members.isEmpty()){
            throw new BusinessException(GroupErrorCode.GROUP_NOT_FOUND);
        }
        this.members = List.copyOf(members);
    }

    public void validateCanRecreateGroup(){
        if(hasPartner()){
            throw new BusinessException(GroupErrorCode.GROUP_PARTNER_ALREADY_EXISTS);
        }
    }

    public void validateCanRecreateInvite(){
        if(hasPartner()){
            throw new BusinessException(GroupErrorCode.GROUP_PARTNER_ALREADY_EXISTS);
        }
    }

    public void validateCanAcceptInvite(){
        if(hasPartner()){
            throw new BusinessException(GroupErrorCode.GROUP_MEMBER_LIMIT_EXCEEDED);
        }
    }

    public boolean hasPartner() {
        return members.size() > 1;
    }

    public List<AppUser> values(){
        return members;
    }
}
