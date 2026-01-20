package com.theradio.presence;

import com.theradio.domain.model.ListeningState;
import com.theradio.domain.model.PlatformConnection;
import com.theradio.domain.model.PlatformType;
import com.theradio.domain.model.User;
import com.theradio.domain.repository.ListeningStateRepository;
import com.theradio.platforms.spotify.SpotifyApiClient;
import com.theradio.platforms.spotify.SpotifyService;
import com.theradio.platforms.spotify.dto.SpotifyCurrentlyPlaying;
import com.theradio.websocket.PresenceWebSocketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ListeningStateService {
    public ListeningStateService(ListeningStateRepository listeningStateRepository, SpotifyService spotifyService, SpotifyApiClient spotifyApiClient, PresenceWebSocketService webSocketService) {
        this.listeningStateRepository = listeningStateRepository;
        this.spotifyService = spotifyService;
        this.spotifyApiClient = spotifyApiClient;
        this.webSocketService = webSocketService;
    }


    private final ListeningStateRepository listeningStateRepository;
    private final SpotifyService spotifyService;
    private final SpotifyApiClient spotifyApiClient;
    private final PresenceWebSocketService webSocketService;

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

        ListeningState savedState = listeningStateRepository.save(state);
        
        // Broadcast update via WebSocket
        webSocketService.broadcastPresenceUpdate(user, savedState);
    }
}

