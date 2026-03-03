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
    public void updateSpotifyPresence(Long userId, Long connectionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        PlatformConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found: " + connectionId));

        updateListeningState(user, connection);
    }

    @Transactional
    public void updateListeningState(User user, PlatformConnection connection) {
        Long userId = user.getId();
        log.info("--- DEEP DEBUG: Polling Spotify for user {} ({}) ---", user.getUsername(), userId);
        
        // 1. Log stored scopes
        log.info("Scopes stored for user {}: {}", userId, connection.getScopes());

        // 2. Log token expiry
        log.info("Access token expires at: {}", connection.getTokenExpiresAt());

        try {
            String accessToken = spotifyService.getValidAccessToken(connection);
            
            // 3. Device Activity Debugging
            log.info("Calling Spotify playback-state API for device info...");
            ResponseEntity<String> playbackResponse = spotifyApiClient.getPlaybackState(accessToken);
            log.info("Playback state status: {}", playbackResponse.getStatusCode());
            if (playbackResponse.getStatusCode().is2xxSuccessful() && playbackResponse.getBody() != null) {
                try {
                    JsonNode pbJson = objectMapper.readTree(playbackResponse.getBody());
                    boolean active = pbJson.path("device").path("is_active").asBoolean();
                    boolean isPlaying = pbJson.path("is_playing").asBoolean();
                    String deviceName = pbJson.path("device").path("name").asText("Unknown");
                    log.info("DEVICE DEBUG: Device active: {}, isPlaying: {}, deviceName: {}", 
                            active, isPlaying, deviceName);
                } catch (Exception e) {
                    log.error("Failed to parse playback state JSON: {}", e.getMessage());
                    log.info("Raw playback body: {}", playbackResponse.getBody());
                }
            } else if (playbackResponse.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.info("DEVICE DEBUG: Spotify returned 204 (No active device context)");
            }

            // 4. Currently Playing Call
            log.info("Calling Spotify currently-playing API...");
            ResponseEntity<String> response = spotifyApiClient.getCurrentlyPlaying(accessToken);
            log.info("Spotify status for user {}: {}", userId, response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.info("Spotify returned 204 (Nothing playing) for user {}", userId);
                webSocketService.broadcastPresencePlaybackState(user, "NO_ACTIVE_PLAYBACK");
                clearListeningState(user);
                return;
            }

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Spotify returned error {} for user {}: {}", 
                        response.getStatusCode(), userId, response.getBody());
                return;
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) {
                log.error("Spotify returned empty body for user {} (status: {})", userId, response.getStatusCode());
                return;
            }

            // 5. Parse and Handle Update
            SpotifyCurrentlyPlaying currentlyPlaying;
            try {
                currentlyPlaying = objectMapper.readValue(body, SpotifyCurrentlyPlaying.class);
            } catch (Exception e) {
                log.error("JSON parsing failed for user {}: {}", userId, e.getMessage());
                log.info("Raw JSON body: {}", body);
                return;
            }

            if (currentlyPlaying == null || currentlyPlaying.getItem() == null) {
                log.warn("Spotify response contains no track item for user {}", userId);
                log.info("Raw JSON body: {}", body);
                clearListeningState(user);
                return;
            }

            SpotifyCurrentlyPlaying.SpotifyTrack track = currentlyPlaying.getItem();
            String artistName = track.getArtists() != null && track.getArtists().length > 0
                    ? track.getArtists()[0].getName()
                    : "Unknown Artist";

            String albumArtUrl = null;
            if (track.getAlbum() != null && track.getAlbum().getImages() != null && track.getAlbum().getImages().length > 0) {
                albumArtUrl = track.getAlbum().getImages()[track.getAlbum().getImages().length - 1].getUrl();
            }

            Optional<ListeningState> existingState = listeningStateRepository.findByUser(user);
            
            ListeningState state;
            if (existingState.isPresent()) {
                state = existingState.get();
                if (track.getId().equals(state.getTrackId()) && state.getIsPlaying() == currentlyPlaying.getIsPlaying()) {
                    state.setProgressMs(currentlyPlaying.getProgressMs() != null ? currentlyPlaying.getProgressMs() : 0);
                    state.setUpdatedAt(java.time.OffsetDateTime.now());
                    listeningStateRepository.save(state);
                    return;
                }
                log.info("User {} track changed: {} - {}", user.getUsername(), track.getName(), artistName);
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

            ListeningState savedState = listeningStateRepository.save(state);
            webSocketService.broadcastPresenceUpdate(user, savedState);

        } catch (Exception e) {
            log.error("Exception in updateListeningState for user {}: {}", userId, e.getMessage(), e);
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

