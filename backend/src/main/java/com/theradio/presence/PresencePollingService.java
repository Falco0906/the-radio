package com.theradio.presence;

import com.theradio.domain.model.PlatformConnection;
import com.theradio.domain.model.PlatformType;
import com.theradio.domain.repository.PlatformConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresencePollingService {
    private static final Logger log = LoggerFactory.getLogger(PresencePollingService.class);

    private static final int INTERVAL_PLAYING = 5000;
    private static final int INTERVAL_PAUSED = 15000;
    private static final int INTERVAL_IDLE = 60000;
    private static final int IDLE_THRESHOLD = 5;

    private final PlatformConnectionRepository connectionRepository;
    private final ListeningStateService listeningStateService;
    private final ActiveUserRegistry activeUserRegistry;

    private final Map<Long, Long> lastPollTime = new ConcurrentHashMap<>();
    private final Map<Long, Integer> pollInterval = new ConcurrentHashMap<>();
    private final Map<Long, Integer> idleCount = new ConcurrentHashMap<>();

    public PresencePollingService(PlatformConnectionRepository connectionRepository,
                                  ListeningStateService listeningStateService,
                                  ActiveUserRegistry activeUserRegistry) {
        this.connectionRepository = connectionRepository;
        this.listeningStateService = listeningStateService;
        this.activeUserRegistry = activeUserRegistry;
    }

    /**
     * Tick every 2 seconds. For each active WebSocket user with a Spotify connection,
     * check if enough time has elapsed based on their adaptive polling interval.
     */
    @Scheduled(fixedDelay = 2000)
    public void pollSpotifyPresence() {
        Set<Long> activeUsers = activeUserRegistry.getActiveUsers();

        if (activeUsers.isEmpty()) {
            return;
        }

        log.debug("Polling tick — {} active WebSocket users", activeUsers.size());

        long now = System.currentTimeMillis();

        // Get all Spotify connections in one query
        List<PlatformConnection> spotifyConnections =
                connectionRepository.findByPlatformWithUser(PlatformType.SPOTIFY);

        for (PlatformConnection connection : spotifyConnections) {
            Long userId = connection.getUser().getId();

            // Only poll users who are connected via WebSocket
            if (!activeUsers.contains(userId)) {
                continue;
            }

            int interval = pollInterval.getOrDefault(userId, INTERVAL_PLAYING);
            long lastPoll = lastPollTime.getOrDefault(userId, 0L);

            if (now - lastPoll < interval) {
                continue;
            }

            log.debug("Polling userId {} (interval={}ms)", userId, interval);

            try {
                boolean hasPlayback = listeningStateService.updateSpotifyPresence(userId, connection.getId());
                adaptInterval(userId, hasPlayback);
            } catch (Exception e) {
                pollInterval.put(userId, INTERVAL_IDLE);
                log.warn("Spotify API failed for userId {}. Backing off polling: {}", userId, e.getMessage());
            }

            lastPollTime.put(userId, now);
        }

        // Clean up tracking maps for disconnected users
        lastPollTime.keySet().retainAll(activeUsers);
        pollInterval.keySet().retainAll(activeUsers);
        idleCount.keySet().retainAll(activeUsers);
    }

    private void adaptInterval(Long userId, boolean hasPlayback) {
        if (!hasPlayback) {
            int count = idleCount.getOrDefault(userId, 0) + 1;
            idleCount.put(userId, count);

            if (count > IDLE_THRESHOLD) {
                if (pollInterval.getOrDefault(userId, INTERVAL_PLAYING) != INTERVAL_IDLE) {
                    log.info("Adjusted polling for userId {} → {}ms (IDLE)", userId, INTERVAL_IDLE);
                }
                pollInterval.put(userId, INTERVAL_IDLE);
            }
            return;
        }

        idleCount.remove(userId);

        // Check the listening state to determine if playing or paused
        // We use a simple heuristic: if updateSpotifyPresence returned true,
        // there's an active playback context. The actual playing/paused state
        // is determined by the isPlaying field set during the update.
        // For simplicity, we default to PLAYING interval when there's active playback.
        int previousInterval = pollInterval.getOrDefault(userId, INTERVAL_PLAYING);
        pollInterval.put(userId, INTERVAL_PLAYING);

        if (previousInterval != INTERVAL_PLAYING) {
            log.info("Adjusted polling for userId {} → {}ms (PLAYING)", userId, INTERVAL_PLAYING);
        }
    }

    public void removeUserState(Long userId) {
        lastPollTime.remove(userId);
        pollInterval.remove(userId);
        idleCount.remove(userId);
    }
}
