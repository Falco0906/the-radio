package com.theradio.domain.repository;

import com.theradio.domain.model.PlatformConnection;
import com.theradio.domain.model.PlatformType;
import com.theradio.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformConnectionRepository extends JpaRepository<PlatformConnection, Long> {
    List<PlatformConnection> findByUser(User user);
    @org.springframework.data.jpa.repository.Query("SELECT pc FROM PlatformConnection pc JOIN FETCH pc.user WHERE pc.platform = :platform")
    List<PlatformConnection> findByPlatformWithUser(@org.springframework.data.repository.query.Param("platform") PlatformType platform);

    List<PlatformConnection> findByPlatform(PlatformType platform);
    Optional<PlatformConnection> findByUserAndPlatform(User user, PlatformType platform);
    Optional<PlatformConnection> findByUserIdAndPlatform(Long userId, PlatformType platform);
    boolean existsByUserAndPlatform(User user, PlatformType platform);
    boolean existsByUserIdAndPlatform(Long userId, PlatformType platform);
}

