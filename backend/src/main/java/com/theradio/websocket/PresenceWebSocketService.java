package com.theradio.websocket;

import com.theradio.domain.model.ListeningState;
import com.theradio.domain.model.User;
import com.theradio.domain.repository.FriendRepository;
import com.theradio.domain.repository.ListeningStateRepository;
import com.theradio.domain.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class PresenceWebSocketService {
    private static final Logger log = LoggerFactory.getLogger(PresenceWebSocketService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final FriendRepository friendRepository;
    private final ListeningStateRepository listeningStateRepository;
    private final UserRepository userRepository;

    public PresenceWebSocketService(SimpMessagingTemplate messagingTemplate, 
                                    FriendRepository friendRepository, 
                                    ListeningStateRepository listeningStateRepository,
                                    UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.friendRepository = friendRepository;
        this.listeningStateRepository = listeningStateRepository;
        this.userRepository = userRepository;
    }

    public void broadcastPresenceUpdate(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("Cannot broadcast: User not found with ID {}", userId);
            return;
        }

        ListeningState updated = listeningStateRepository.findByUserId(userId).orElse(null);
        if (updated == null) {
            log.warn("Cannot broadcast: No ListeningState found for user {}", userId);
            return;
        }

        log.info("Broadcasting presence payload: isPlaying={}, track={}",
                updated.getIsPlaying(),
                updated.getTrackName());

        // Create presence update message
        PresenceUpdateMessage message = PresenceUpdateMessage.builder()
                .type("PRESENCE_UPDATE")
                .userId(userId)
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .platform(updated.getPlatform().name())
                .trackId(updated.getTrackId())
                .trackName(updated.getTrackName())
                .artist(updated.getArtist())
                .albumArtUrl(updated.getAlbumArtUrl())
                .progressMs(updated.getProgressMs())
                .durationMs(updated.getDurationMs())
                .isPlaying(updated.getIsPlaying())
                .updatedAt(updated.getUpdatedAt())
                .build();

        // Broadcast to specific user topic
        String topic = "/topic/presence/" + userId;
        messagingTemplate.convertAndSend(topic, message);
        log.info("Sending WS message to: {}", topic);

        // TEST TOPIC: Hardcoded for global debugging
        messagingTemplate.convertAndSend("/topic/presence-test", message);
        log.info("Sending WS message to: /topic/presence-test");
    }

    public void broadcastPresenceUpdate(User user, ListeningState state) {
        // Redirect to ID-based broadcast for consistency and fresh fetch
        broadcastPresenceUpdate(user.getId());
    }

    public void broadcastPresencePlaybackState(User user, String status) {
        // Get all friends of this user
        List<User> friends = friendRepository.findByUser(user).stream()
                .map(f -> f.getFriend())
                .toList();

        PresenceUpdateMessage message = PresenceUpdateMessage.builder()
                .type("PLAYBACK_STATE_UPDATE")
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .platform("SPOTIFY") // Defaulting to Spotify for this debug mode
                .trackName(status) // Reuse trackName to carry the status message for visibility
                .isPlaying(false)
                .build();

        for (User friend : friends) {
            messagingTemplate.convertAndSendToUser(
                    friend.getUsername(),
                    "/queue/presence",
                    message
            );
        }

        // Also send to the user themselves
        messagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/presence",
                message
        );
    }

    public void broadcastPresenceOffline(User user) {
        // Get all friends of this user
        List<User> friends = friendRepository.findByUser(user).stream()
                .map(f -> f.getFriend())
                .toList();

        PresenceUpdateMessage message = PresenceUpdateMessage.builder()
                .type("PRESENCE_OFFLINE")
                .userId(user.getId())
                .username(user.getUsername())
                .build();

        for (User friend : friends) {
            messagingTemplate.convertAndSendToUser(
                    friend.getUsername(),
                    "/queue/presence",
                    message
            );
        }
    }

    public void broadcastPresenceOnline(User user) {
        // Get all friends of this user
        List<User> friends = friendRepository.findByUser(user).stream()
                .map(f -> f.getFriend())
                .toList();

        PresenceUpdateMessage message = PresenceUpdateMessage.builder()
                .type("PRESENCE_ONLINE")
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .build();

        for (User friend : friends) {
            messagingTemplate.convertAndSendToUser(
                    friend.getUsername(),
                    "/queue/presence",
                    message
            );
        }
    }

    public static class PresenceUpdateMessage {
        private String type; // PRESENCE_UPDATE, PRESENCE_OFFLINE, PRESENCE_ONLINE
        private Long userId;
        private String username;
        private String displayName;
        private String platform;
        private String trackId;
        private String trackName;
        private String artist;
        private String albumArtUrl;
        private Integer progressMs;
        private Integer durationMs;
        private Boolean isPlaying;
        private java.time.OffsetDateTime updatedAt;

        public static PresenceUpdateMessageBuilder builder() {
            return new PresenceUpdateMessageBuilder();
        }

        public static class PresenceUpdateMessageBuilder {
            private String type;
            private Long userId;
            private String username;
            private String displayName;
            private String platform;
            private String trackId;
            private String trackName;
            private String artist;
            private String albumArtUrl;
            private Integer progressMs;
            private Integer durationMs;
            private Boolean isPlaying;
            private java.time.OffsetDateTime updatedAt;

            public PresenceUpdateMessageBuilder type(String type) { this.type = type; return this; }
            public PresenceUpdateMessageBuilder userId(Long userId) { this.userId = userId; return this; }
            public PresenceUpdateMessageBuilder username(String username) { this.username = username; return this; }
            public PresenceUpdateMessageBuilder displayName(String displayName) { this.displayName = displayName; return this; }
            public PresenceUpdateMessageBuilder platform(String platform) { this.platform = platform; return this; }
            public PresenceUpdateMessageBuilder trackId(String trackId) { this.trackId = trackId; return this; }
            public PresenceUpdateMessageBuilder trackName(String trackName) { this.trackName = trackName; return this; }
            public PresenceUpdateMessageBuilder artist(String artist) { this.artist = artist; return this; }
            public PresenceUpdateMessageBuilder albumArtUrl(String albumArtUrl) { this.albumArtUrl = albumArtUrl; return this; }
            public PresenceUpdateMessageBuilder progressMs(Integer progressMs) { this.progressMs = progressMs; return this; }
            public PresenceUpdateMessageBuilder durationMs(Integer durationMs) { this.durationMs = durationMs; return this; }
            public PresenceUpdateMessageBuilder isPlaying(Boolean isPlaying) { this.isPlaying = isPlaying; return this; }
            public PresenceUpdateMessageBuilder updatedAt(java.time.OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

            public PresenceUpdateMessage build() {
                PresenceUpdateMessage msg = new PresenceUpdateMessage();
                msg.type = type;
                msg.userId = userId;
                msg.username = username;
                msg.displayName = displayName;
                msg.platform = platform;
                msg.trackId = trackId;
                msg.trackName = trackName;
                msg.artist = artist;
                msg.albumArtUrl = albumArtUrl;
                msg.progressMs = progressMs;
                msg.durationMs = durationMs;
                msg.isPlaying = isPlaying;
                msg.updatedAt = updatedAt;
                return msg;
            }
        }

        // Getters
        public String getType() { return type; }
        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getDisplayName() { return displayName; }
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
}

