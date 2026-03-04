package com.theradio.presence;

import com.theradio.domain.model.ListeningState;
import com.theradio.domain.model.PlatformConnection;
import com.theradio.domain.model.PlatformType;
import com.theradio.domain.model.User;
import com.theradio.domain.repository.ListeningStateRepository;
import com.theradio.domain.repository.PlatformConnectionRepository;
import com.theradio.domain.repository.UserRepository;
import com.theradio.platforms.spotify.SpotifyApiClient;
import com.theradio.platforms.spotify.SpotifyService;
import com.theradio.platforms.spotify.dto.SpotifyCurrentlyPlaying;
import com.theradio.platforms.soundcloud.SoundCloudApiClient;
import com.theradio.platforms.soundcloud.SoundCloudService;
import com.theradio.websocket.PresenceWebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ListeningStateService {
    private static final Logger log = LoggerFactory.getLogger(ListeningStateService.class);

    private final ListeningStateRepository listeningStateRepository;
    private final PlatformConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final SpotifyService spotifyService;
    private final SpotifyApiClient spotifyApiClient;
    private final SoundCloudService soundCloudService;
    private final SoundCloudApiClient soundCloudApiClient;
    private final PresenceWebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    public ListeningStateService(ListeningStateRepository listeningStateRepository, 
                                 PlatformConnectionRepository connectionRepository,
                                 UserRepository userRepository,
                                 SpotifyService spotifyService, 
                                 SpotifyApiClient spotifyApiClient,
                                 SoundCloudService soundCloudService,
                                 SoundCloudApiClient soundCloudApiClient,
                                 PresenceWebSocketService webSocketService,
                                 ObjectMapper objectMapper) {
        this.listeningStateRepository = listeningStateRepository;
        this.connectionRepository = connectionRepository;
        this.userRepository = userRepository;
        this.spotifyService = spotifyService;
        this.spotifyApiClient = spotifyApiClient;
        this.soundCloudService = soundCloudService;
        this.soundCloudApiClient = soundCloudApiClient;
        this.webSocketService = webSocketService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void refreshUserPresence(User user) {
        if (!user.getIsLive()) {
            listeningStateRepository.findByUser(user).ifPresent(listeningStateRepository::delete);
            webSocketService.broadcastPresenceOffline(user);
            return;
        }

        // Fetch all platform connections for this user
        // Note: For now we handle Spotify and SoundCloud
        List<PlatformConnection> connections = connectionRepository.findByUser(user);
        
        for (PlatformConnection connection : connections) {
            if (connection.getPlatform() == PlatformType.SPOTIFY) {
                updateListeningState(user, connection);
            } else if (connection.getPlatform() == PlatformType.SOUNDCLOUD) {
                updateSoundCloudListeningState(user, connection);
            }
        }
    }

    @Transactional
    public boolean updateSpotifyPresence(Long userId, Long connectionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        PlatformConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found: " + connectionId));

        return updateListeningState(user, connection);
    }

    @Transactional
    public boolean updateListeningState(User user, PlatformConnection connection) {
        Long userId = user.getId();
        log.debug("Polling Spotify for user {} ({})", user.getUsername(), userId);

        try {
            String accessToken = spotifyService.getValidAccessToken(connection);
            
            // Device Activity Debugging
            log.debug("Calling Spotify playback-state API for device info...");
            ResponseEntity<String> playbackResponse = spotifyApiClient.getPlaybackState(accessToken);
            log.debug("Playback state status: {}", playbackResponse.getStatusCode());
            if (playbackResponse.getStatusCode().is2xxSuccessful() && playbackResponse.getBody() != null) {
                try {
                    JsonNode pbJson = objectMapper.readTree(playbackResponse.getBody());
                    boolean active = pbJson.path("device").path("is_active").asBoolean();
                    boolean isPlaying = pbJson.path("is_playing").asBoolean();
                    String deviceName = pbJson.path("device").path("name").asText("Unknown");
                    log.debug("DEVICE: active={}, playing={}, device={}", 
                            active, isPlaying, deviceName);
                } catch (Exception e) {
                    log.error("Failed to parse playback state JSON: {}", e.getMessage());
                }
            } else if (playbackResponse.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.debug("Spotify returned 204 (No active device context)");
            }

            // Currently Playing Call
            ResponseEntity<String> response = spotifyApiClient.getCurrentlyPlaying(accessToken);
            log.debug("Spotify currently-playing status for user {}: {}", userId, response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.debug("Nothing playing for user {}", userId);
                webSocketService.broadcastPresencePlaybackState(user, "NO_ACTIVE_PLAYBACK");
                clearListeningState(user);
                return false;
            }

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Spotify returned error {} for user {}: {}", 
                        response.getStatusCode(), userId, response.getBody());
                return false;
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) {
                log.error("Spotify returned empty body for user {} (status: {})", userId, response.getStatusCode());
                return false;
            }

            // Parse and Handle Update
            SpotifyCurrentlyPlaying currentlyPlaying;
            try {
                currentlyPlaying = objectMapper.readValue(body, SpotifyCurrentlyPlaying.class);
            } catch (Exception e) {
                log.error("JSON parsing failed for user {}: {}", userId, e.getMessage());
                return false;
            }

            if (currentlyPlaying == null || currentlyPlaying.getItem() == null) {
                log.warn("Spotify response contains no track item for user {}", userId);
                clearListeningState(user);
                return false;
            }

            SpotifyCurrentlyPlaying.SpotifyTrack track = currentlyPlaying.getItem();
            String artistName = track.getArtists() != null && track.getArtists().length > 0
                    ? track.getArtists()[0].getName()
                    : "Unknown Artist";
            String trackName = track.getName();

            log.info("User {} listening: {} - {}", user.getUsername(), artistName, trackName);

            String albumArtUrl = null;
            if (track.getAlbum() != null && track.getAlbum().getImages() != null && track.getAlbum().getImages().length > 0) {
                albumArtUrl = track.getAlbum().getImages()[track.getAlbum().getImages().length - 1].getUrl();
            }

            Optional<ListeningState> existingState = listeningStateRepository.findByUser(user);
            
            ListeningState state;
            if (existingState.isPresent()) {
                state = existingState.get();
                state.setPlatform(PlatformType.SPOTIFY);
            } else {
                log.info("User {} started playing: {} - {}", user.getUsername(), track.getName(), artistName);
                state = ListeningState.builder()
                        .user(user)
                        .platform(PlatformType.SPOTIFY)
                        .build();
            }

            state.setTrackId(track.getId());
            state.setTrackName(track.getName());
            state.setArtist(artistName);
            state.setAlbumArtUrl(albumArtUrl);
            state.setProgressMs(currentlyPlaying.getProgressMs() != null ? currentlyPlaying.getProgressMs() : 0);
            state.setDurationMs(track.getDurationMs());
            state.setIsPlaying(currentlyPlaying.getIsPlaying() != null ? currentlyPlaying.getIsPlaying() : false);
            state.setUpdatedAt(java.time.OffsetDateTime.now());

            ListeningState savedState = listeningStateRepository.saveAndFlush(state);
            log.debug("Saved listening state to DB for user {}", userId);
            
            // Explicitly broadcast by ID every time
            webSocketService.broadcastPresenceUpdate(userId);

            return true;

        } catch (Exception e) {
            log.error("Exception in updateListeningState for user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    private void clearListeningState(User user) {
        listeningStateRepository.findByUser(user).ifPresent(state -> {
            log.info("Clearing listening state for user {}", user.getUsername());
            listeningStateRepository.delete(state);
            webSocketService.broadcastPresenceOffline(user);
        });
    }

    @Transactional
    public void updateSoundCloudListeningState(User user, PlatformConnection connection) {
        if (!user.getIsLive()) {
            listeningStateRepository.findByUser(user).ifPresent(listeningStateRepository::delete);
            return;
        }

        try {
            List<Map<String, Object>> tracks = soundCloudApiClient.getCurrentlyPlaying(connection.getAccessToken());
            
            if (tracks == null || tracks.isEmpty()) {
                return;
            }

            Map<String, Object> track = tracks.get(0);

            @SuppressWarnings("unchecked")
            Map<String, Object> trackUser = (Map<String, Object>) track.get("user");
            String artistName = trackUser != null ? (String) trackUser.get("username") : "Unknown Artist";

            Optional<ListeningState> existingState = listeningStateRepository.findByUser(user);
            
            ListeningState state = existingState.orElseGet(() -> ListeningState.builder()
                    .user(user)
                    .platform(PlatformType.SOUNDCLOUD)
                    .build());
            
            state.setPlatform(PlatformType.SOUNDCLOUD);

            state.setTrackId(String.valueOf(track.get("id")));
            state.setTrackName((String) track.get("title"));
            state.setArtist(artistName);
            state.setAlbumArtUrl((String) track.get("artwork_url"));
            state.setProgressMs(0); 
            state.setDurationMs(track.get("duration") instanceof Number 
                    ? ((Number) track.get("duration")).intValue() 
                    : 0);
            state.setIsPlaying(true); 
            state.setUpdatedAt(java.time.OffsetDateTime.now());

            ListeningState savedState = listeningStateRepository.save(state);
            webSocketService.broadcastPresenceUpdate(user, savedState);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(ListeningStateService.class).error("Error updating SoundCloud state for user {}: {}", user.getId(), e.getMessage());
        }
    }
}

