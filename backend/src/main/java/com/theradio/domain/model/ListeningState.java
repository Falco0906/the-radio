package com.theradio.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "listening_state")
public class ListeningState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlatformType platform;

    @Column(name = "track_id")
    private String trackId;

    @Column(name = "track_name", length = 500)
    private String trackName;

    @Column(length = 500)
    private String artist;

    @Column(name = "album_art_url", columnDefinition = "TEXT")
    private String albumArtUrl;

    @Column(name = "progress_ms")
    private Integer progressMs = 0;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "is_playing", nullable = false)
    private Boolean isPlaying = false;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public ListeningState() {}

    public ListeningState(Long id, User user, PlatformType platform, String trackId, String trackName,
                         String artist, String albumArtUrl, Integer progressMs, Integer durationMs,
                         Boolean isPlaying, OffsetDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.platform = platform;
        this.trackId = trackId;
        this.trackName = trackName;
        this.artist = artist;
        this.albumArtUrl = albumArtUrl;
        this.progressMs = progressMs;
        this.durationMs = durationMs;
        this.isPlaying = isPlaying;
        this.updatedAt = updatedAt;
    }

    public static ListeningStateBuilder builder() {
        return new ListeningStateBuilder();
    }

    public static class ListeningStateBuilder {
        private Long id;
        private User user;
        private PlatformType platform;
        private String trackId;
        private String trackName;
        private String artist;
        private String albumArtUrl;
        private Integer progressMs = 0;
        private Integer durationMs;
        private Boolean isPlaying = false;
        private OffsetDateTime updatedAt = OffsetDateTime.now();

        public ListeningStateBuilder id(Long id) { this.id = id; return this; }
        public ListeningStateBuilder user(User user) { this.user = user; return this; }
        public ListeningStateBuilder platform(PlatformType platform) { this.platform = platform; return this; }
        public ListeningStateBuilder trackId(String trackId) { this.trackId = trackId; return this; }
        public ListeningStateBuilder trackName(String trackName) { this.trackName = trackName; return this; }
        public ListeningStateBuilder artist(String artist) { this.artist = artist; return this; }
        public ListeningStateBuilder albumArtUrl(String albumArtUrl) { this.albumArtUrl = albumArtUrl; return this; }
        public ListeningStateBuilder progressMs(Integer progressMs) { this.progressMs = progressMs; return this; }
        public ListeningStateBuilder durationMs(Integer durationMs) { this.durationMs = durationMs; return this; }
        public ListeningStateBuilder isPlaying(Boolean isPlaying) { this.isPlaying = isPlaying; return this; }
        public ListeningStateBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ListeningState build() {
            return new ListeningState(id, user, platform, trackId, trackName, artist, albumArtUrl, progressMs, durationMs, isPlaying, updatedAt);
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public PlatformType getPlatform() { return platform; }
    public void setPlatform(PlatformType platform) { this.platform = platform; }
    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }
    public String getTrackName() { return trackName; }
    public void setTrackName(String trackName) { this.trackName = trackName; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getAlbumArtUrl() { return albumArtUrl; }
    public void setAlbumArtUrl(String albumArtUrl) { this.albumArtUrl = albumArtUrl; }
    public Integer getProgressMs() { return progressMs; }
    public void setProgressMs(Integer progressMs) { this.progressMs = progressMs; }
    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }
    public Boolean getIsPlaying() { return isPlaying; }
    public void setIsPlaying(Boolean isPlaying) { this.isPlaying = isPlaying; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
