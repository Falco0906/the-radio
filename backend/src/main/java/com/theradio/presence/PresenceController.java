package com.theradio.presence;

import com.theradio.auth.AuthService;
import com.theradio.domain.model.ListeningState;
import com.theradio.domain.model.User;
import com.theradio.domain.repository.ListeningStateRepository;
import com.theradio.domain.repository.UserRepository;
import com.theradio.websocket.PresenceWebSocketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {
    public PresenceController(AuthService authService, ListeningStateRepository listeningStateRepository, UserRepository userRepository, PresenceWebSocketService webSocketService) {
        this.authService = authService;
        this.listeningStateRepository = listeningStateRepository;
        this.userRepository = userRepository;
        this.webSocketService = webSocketService;
    }


    private final AuthService authService;
    private final ListeningStateRepository listeningStateRepository;
    private final UserRepository userRepository;
    private final PresenceWebSocketService webSocketService;

    @GetMapping("/current")
    public ResponseEntity<ListeningStateDto> getCurrentPresence() {
        User currentUser = authService.getCurrentUser();
        
        if (!currentUser.getIsLive()) {
            return ResponseEntity.ok().build();
        }

        return listeningStateRepository.findByUser(currentUser)
                .map(state -> ResponseEntity.ok(ListeningStateDto.from(state)))
                .orElse(ResponseEntity.ok().build());
    }

    @PutMapping("/live")
    public ResponseEntity<Map<String, Boolean>> toggleLive(@RequestBody ToggleLiveRequest request) {
        User currentUser = authService.getCurrentUser();
        boolean wasLive = currentUser.getIsLive();
        currentUser.setIsLive(request.getIsLive());
        userRepository.save(currentUser);
        
        // Broadcast presence change
        if (request.getIsLive() && !wasLive) {
            // User went live
            listeningStateRepository.findByUser(currentUser).ifPresent(state -> {
                webSocketService.broadcastPresenceUpdate(currentUser, state);
            });
            webSocketService.broadcastPresenceOnline(currentUser);
        } else if (!request.getIsLive() && wasLive) {
            // User went invisible
            webSocketService.broadcastPresenceOffline(currentUser);
        }
        
        return ResponseEntity.ok(Map.of("isLive", currentUser.getIsLive()));
    }

    public static class ListeningStateDto {
        private Long userId;
        private String platform;
        private String trackId;
        private String trackName;
        private String artist;
        private String albumArtUrl;
        private Integer progressMs;
        private Integer durationMs;
        private Boolean isPlaying;
        private java.time.OffsetDateTime updatedAt;

        public static ListeningStateDtoBuilder builder() {
            return new ListeningStateDtoBuilder();
        }

        public static class ListeningStateDtoBuilder {
            private Long userId;
            private String platform;
            private String trackId;
            private String trackName;
            private String artist;
            private String albumArtUrl;
            private Integer progressMs;
            private Integer durationMs;
            private Boolean isPlaying;
            private java.time.OffsetDateTime updatedAt;

            public ListeningStateDtoBuilder userId(Long userId) { this.userId = userId; return this; }
            public ListeningStateDtoBuilder platform(String platform) { this.platform = platform; return this; }
            public ListeningStateDtoBuilder trackId(String trackId) { this.trackId = trackId; return this; }
            public ListeningStateDtoBuilder trackName(String trackName) { this.trackName = trackName; return this; }
            public ListeningStateDtoBuilder artist(String artist) { this.artist = artist; return this; }
            public ListeningStateDtoBuilder albumArtUrl(String albumArtUrl) { this.albumArtUrl = albumArtUrl; return this; }
            public ListeningStateDtoBuilder progressMs(Integer progressMs) { this.progressMs = progressMs; return this; }
            public ListeningStateDtoBuilder durationMs(Integer durationMs) { this.durationMs = durationMs; return this; }
            public ListeningStateDtoBuilder isPlaying(Boolean isPlaying) { this.isPlaying = isPlaying; return this; }
            public ListeningStateDtoBuilder updatedAt(java.time.OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

            public ListeningStateDto build() {
                ListeningStateDto dto = new ListeningStateDto();
                dto.userId = userId;
                dto.platform = platform;
                dto.trackId = trackId;
                dto.trackName = trackName;
                dto.artist = artist;
                dto.albumArtUrl = albumArtUrl;
                dto.progressMs = progressMs;
                dto.durationMs = durationMs;
                dto.isPlaying = isPlaying;
                dto.updatedAt = updatedAt;
                return dto;
            }
        }

        public static ListeningStateDto from(ListeningState state) {
            return ListeningStateDto.builder()
                    .userId(state.getUser().getId())
                    .platform(state.getPlatform().name())
                    .trackId(state.getTrackId())
                    .trackName(state.getTrackName())
                    .artist(state.getArtist())
                    .albumArtUrl(state.getAlbumArtUrl())
                    .progressMs(state.getProgressMs())
                    .durationMs(state.getDurationMs())
                    .isPlaying(state.getIsPlaying())
                    .updatedAt(state.getUpdatedAt())
                    .build();
        }

        public Long getUserId() { return userId; }
        public String getPlatform() { return platform; }
        public String getTrackId() { return trackId; }
        public String getTrackName() { return trackName; }
        public String getArtist() { return artist; }
        public String getAlbumArtUrl() { return albumArtUrl; }
        public Integer getProgressMs() { return progressMs; }
        public Integer getDurationMs() { return durationMs; }
        public Boolean getIsPlaying() { return isPlaying; }
        public java.time.OffsetDateTime getUpdatedAt() { return updatedAt; }
    }

    public static class ToggleLiveRequest {
        private Boolean isLive;

        public Boolean getIsLive() { return isLive; }
        public void setIsLive(Boolean isLive) { this.isLive = isLive; }
    }
}

