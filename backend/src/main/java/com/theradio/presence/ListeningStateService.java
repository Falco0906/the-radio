package com.theradio.presence;

import com.theradio.domain.model.ListeningState;
import com.theradio.domain.model.PlatformConnection;
import com.theradio.domain.model.PlatformType;
import com.theradio.domain.model.User;
import com.theradio.domain.repository.ListeningStateRepository;
import com.theradio.platforms.spotify.SpotifyApiClient;
import com.theradio.platforms.spotify.SpotifyService;
import com.theradio.platforms.spotify.dto.SpotifyCurrentlyPlaying;
import com.theradio.platforms.soundcloud.SoundCloudApiClient;
import com.theradio.platforms.soundcloud.SoundCloudService;
import com.theradio.websocket.PresenceWebSocketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ListeningStateService {
    private final ListeningStateRepository listeningStateRepository;
    private final SpotifyService spotifyService;
    private final SpotifyApiClient spotifyApiClient;
    private final SoundCloudService soundCloudService;
    private final SoundCloudApiClient soundCloudApiClient;
    private final PresenceWebSocketService webSocketService;

    public ListeningStateService(ListeningStateRepository listeningStateRepository, 
                                SpotifyService spotifyService, 
                                SpotifyApiClient spotifyApiClient,
                                SoundCloudService soundCloudService,
                                SoundCloudApiClient soundCloudApiClient,
                                PresenceWebSocketService webSocketService) {
        this.listeningStateRepository = listeningStateRepository;
        this.spotifyService = spotifyService;
        this.spotifyApiClient = spotifyApiClient;
        this.soundCloudService = soundCloudService;
        this.soundCloudApiClient = soundCloudApiClient;
        this.webSocketService = webSocketService;
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
        com.theradio.domain.repository.PlatformConnectionRepository connectionRepository = 
            org.springframework.context.ApplicationContextHolder.getContext().getBean(com.theradio.domain.repository.PlatformConnectionRepository.class);
        
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
    public void updateListeningState(User user, PlatformConnection connection) {
        // Only update if user is live
        if (!user.getIsLive()) {
            // Clear listening state if user goes invisible
            boolean hadState = listeningStateRepository.findByUser(user).isPresent();
            listeningStateRepository.findByUser(user).ifPresent(listeningStateRepository::delete);
            if (hadState) {
                webSocketService.broadcastPresenceOffline(user);
            }
            return;
        }

        try {
            String accessToken = spotifyService.getValidAccessToken(connection);
            SpotifyCurrentlyPlaying currentlyPlaying = spotifyApiClient.getCurrentlyPlaying(accessToken);

            if (currentlyPlaying == null || currentlyPlaying.getItem() == null) {
                // No track playing, clear state
                listeningStateRepository.findByUser(user).ifPresent(listeningStateRepository::delete);
                return;
            }

            SpotifyCurrentlyPlaying.SpotifyTrack track = currentlyPlaying.getItem();
            String artistName = track.getArtists() != null && track.getArtists().length > 0
                    ? track.getArtists()[0].getName()
                    : "Unknown Artist";

            String albumArtUrl = null;
            if (track.getAlbum() != null && track.getAlbum().getImages() != null && track.getAlbum().getImages().length > 0) {
                // Get smallest image (usually last in array)
                albumArtUrl = track.getAlbum().getImages()[track.getAlbum().getImages().length - 1].getUrl();
            }

            Optional<ListeningState> existingState = listeningStateRepository.findByUser(user);
            
            ListeningState state;
            if (existingState.isPresent()) {
                state = existingState.get();
                state.setPlatform(PlatformType.SPOTIFY); // Update platform in case they switched
            } else {
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
            
            // Broadcast update via WebSocket
            webSocketService.broadcastPresenceUpdate(user, savedState);
        } catch (Exception e) {
            // Log error but don't crash
            org.slf4j.LoggerFactory.getLogger(ListeningStateService.class).error("Error updating Spotify state for user {}: {}", user.getId(), e.getMessage());
        }
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

