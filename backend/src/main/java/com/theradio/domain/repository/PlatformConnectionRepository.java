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
    Optional<PlatformConnection> findByUserAndPlatform(User user, PlatformType platform);
    boolean existsByUserAndPlatform(User user, PlatformType platform);
}

