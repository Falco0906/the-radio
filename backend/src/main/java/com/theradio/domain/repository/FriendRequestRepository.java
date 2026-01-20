package com.theradio.domain.repository;

import com.theradio.domain.model.FriendRequest;
import com.theradio.domain.model.FriendRequestStatus;
import com.theradio.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    Optional<FriendRequest> findByRequesterAndRecipient(User requester, User recipient);
    List<FriendRequest> findByRequesterAndStatus(User requester, FriendRequestStatus status);
    List<FriendRequest> findByRecipientAndStatus(User recipient, FriendRequestStatus status);
    boolean existsByRequesterAndRecipient(User requester, User recipient);
}

