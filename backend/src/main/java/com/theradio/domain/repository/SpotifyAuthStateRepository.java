package com.theradio.domain.repository;

import com.theradio.domain.model.SpotifyAuthState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SpotifyAuthStateRepository extends JpaRepository<SpotifyAuthState, Long> {
    Optional<SpotifyAuthState> findByState(String state);
    void deleteByState(String state);
}
