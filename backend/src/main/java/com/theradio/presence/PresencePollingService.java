package com.theradio.presence;

import com.theradio.domain.model.PlatformConnection;
import com.theradio.domain.model.PlatformType;
import com.theradio.domain.model.User;
import com.theradio.domain.repository.PlatformConnectionRepository;
import com.theradio.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PresencePollingService {
    private static final Logger log = LoggerFactory.getLogger(PresencePollingService.class);

    private final UserRepository userRepository;
    private final PlatformConnectionRepository connectionRepository;
    private final ListeningStateService listeningStateService;

    public PresencePollingService(UserRepository userRepository,
                                  PlatformConnectionRepository connectionRepository,
                                  ListeningStateService listeningStateService) {
        this.userRepository = userRepository;
        this.connectionRepository = connectionRepository;
        this.listeningStateService = listeningStateService;
    }

    /**
     * Poll Spotify presence every 7 seconds.
     * We only poll users who have a Spotify connection.
     */
    @Scheduled(fixedDelay = 7000)
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public void pollSpotifyPresence() {
        log.trace("Polling Spotify presence for all connected users");
        
        List<PlatformConnection> spotifyConnections = connectionRepository.findByPlatformWithUser(PlatformType.SPOTIFY);
        
        for (PlatformConnection connection : spotifyConnections) {
            User user = connection.getUser();
            try {
                // This will use the refresh token if the access token is expired
                listeningStateService.updateListeningState(user, connection);
            } catch (Exception e) {
                log.error("Failed to poll Spotify presence for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }
}
