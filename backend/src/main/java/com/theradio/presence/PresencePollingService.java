package com.theradio.presence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.theradio.domain.model.PlatformConnection;
import com.theradio.domain.model.PlatformType;
import com.theradio.domain.model.User;
import com.theradio.domain.repository.PlatformConnectionRepository;
import com.theradio.domain.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PresencePollingService {
    private static final Logger log = LoggerFactory.getLogger(com.theradio.presence.PresencePollingService.class);

    public PresencePollingService(UserRepository userRepository, PlatformConnectionRepository platformConnectionRepository, ListeningStateService listeningStateService) {
        this.userRepository = userRepository;
        this.platformConnectionRepository = platformConnectionRepository;
        this.listeningStateService = listeningStateService;
    }


    private final UserRepository userRepository;
    private final PlatformConnectionRepository platformConnectionRepository;
    private final ListeningStateService listeningStateService;

    @Scheduled(fixedRateString = "${app.spotify.polling-interval-seconds:5}000")
    @Transactional
    public void pollAllUsers() {
        // Get all users who are live and have platform connections
        List<User> liveUsers = userRepository.findAll().stream()
                .filter(User::getIsLive)
                .toList();

        for (User user : liveUsers) {
            try {
                List<PlatformConnection> connections = platformConnectionRepository.findByUser(user);
                
                // For now, only handle Spotify
                PlatformConnection spotifyConnection = connections.stream()
                        .filter(c -> c.getPlatform() == PlatformType.SPOTIFY)
                        .findFirst()
                        .orElse(null);

                if (spotifyConnection != null) {
                    listeningStateService.updateListeningState(user, spotifyConnection);
                }
            } catch (Exception e) {
                log.error("Error polling presence for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }
}

