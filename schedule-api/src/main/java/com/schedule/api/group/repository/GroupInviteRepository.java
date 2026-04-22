package com.schedule.api.group.repository;

import com.schedule.api.group.domain.GroupInvite;
import com.schedule.api.group.domain.InviteStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, String> {

    Optional<GroupInvite> findByCode(String code);

    Optional<GroupInvite> findByInviteToken(String inviteToken);

    Optional<GroupInvite> findFirstByGroupIdAndStatusOrderByCreatedAtDesc(String groupId, InviteStatus status);
}
