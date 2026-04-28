package com.schedule.api.notification.repository;

import com.schedule.api.notification.domain.FcmDeviceToken;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FcmDeviceTokenRepository extends JpaRepository<FcmDeviceToken, String> {

    List<FcmDeviceToken> findAllByUserIdIn(Collection<String> userIds);

    List<FcmDeviceToken> findAllByGroupId(String groupId);
}
