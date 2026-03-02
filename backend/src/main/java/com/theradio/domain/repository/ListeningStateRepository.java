package com.theradio.domain.repository;

import com.theradio.domain.model.ListeningState;
import com.theradio.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListeningStateRepository extends JpaRepository<ListeningState, Long> {
    Optional<ListeningState> findByUser(User user);
    List<ListeningState> findByUserIn(List<User> users);
    void deleteByUser(User user);
}

