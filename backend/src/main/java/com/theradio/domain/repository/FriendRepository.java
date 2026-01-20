package com.theradio.domain.repository;

import com.theradio.domain.model.Friend;
import com.theradio.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {
    List<Friend> findByUser(User user);
    Optional<Friend> findByUserAndFriend(User user, User friend);
    boolean existsByUserAndFriend(User user, User friend);
    void deleteByUserAndFriend(User user, User friend);
}

