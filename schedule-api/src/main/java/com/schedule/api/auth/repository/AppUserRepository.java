package com.schedule.api.auth.repository;

import com.schedule.api.auth.domain.AppUser;
import com.schedule.api.auth.domain.OAuthProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, String> {

    Optional<AppUser> findByOauthProviderAndOauthProviderUserId(OAuthProvider oauthProvider, String oauthProviderUserId);

    List<AppUser> findAllByGroupIdOrderByCreatedAtAsc(String groupId);
}
