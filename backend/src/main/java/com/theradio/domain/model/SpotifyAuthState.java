package com.theradio.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "spotify_auth_states")
public class SpotifyAuthState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String state;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    public SpotifyAuthState() {}

    public SpotifyAuthState(Long id, String state, Long userId, OffsetDateTime expiresAt) {
        this.id = id;
        this.state = state;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public static SpotifyAuthStateBuilder builder() {
        return new SpotifyAuthStateBuilder();
    }

    public static class SpotifyAuthStateBuilder {
        private Long id;
        private String state;
        private Long userId;
        private OffsetDateTime expiresAt;

        public SpotifyAuthStateBuilder id(Long id) { this.id = id; return this; }
        public SpotifyAuthStateBuilder state(String state) { this.state = state; return this; }
        public SpotifyAuthStateBuilder userId(Long userId) { this.userId = userId; return this; }
        public SpotifyAuthStateBuilder expiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; return this; }

        public SpotifyAuthState build() {
            return new SpotifyAuthState(id, state, userId, expiresAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
}
