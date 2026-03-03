package com.theradio.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_platform_connections", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "platform"}))
public class PlatformConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlatformType platform;

    @Column(name = "platform_user_id", nullable = false)
    private String platformUserId;

    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expires_at")
    private OffsetDateTime tokenExpiresAt;

    @Column(name = "scopes", columnDefinition = "TEXT")
    private String scopes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public PlatformConnection() {}

    public PlatformConnection(Long id, User user, PlatformType platform, String platformUserId, 
                             String accessToken, String refreshToken, OffsetDateTime tokenExpiresAt,
                             String scopes, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.platform = platform;
        this.platformUserId = platformUserId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiresAt = tokenExpiresAt;
        this.scopes = scopes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PlatformConnectionBuilder builder() {
        return new PlatformConnectionBuilder();
    }

    public static class PlatformConnectionBuilder {
        private Long id;
        private User user;
        private PlatformType platform;
        private String platformUserId;
        private String accessToken;
        private String refreshToken;
        private OffsetDateTime tokenExpiresAt;
        private String scopes;
        private OffsetDateTime createdAt = OffsetDateTime.now();
        private OffsetDateTime updatedAt = OffsetDateTime.now();

        public PlatformConnectionBuilder id(Long id) { this.id = id; return this; }
        public PlatformConnectionBuilder user(User user) { this.user = user; return this; }
        public PlatformConnectionBuilder platform(PlatformType platform) { this.platform = platform; return this; }
        public PlatformConnectionBuilder platformUserId(String platformUserId) { this.platformUserId = platformUserId; return this; }
        public PlatformConnectionBuilder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public PlatformConnectionBuilder refreshToken(String refreshToken) { this.refreshToken = refreshToken; return this; }
        public PlatformConnectionBuilder tokenExpiresAt(OffsetDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; return this; }
        public PlatformConnectionBuilder scopes(String scopes) { this.scopes = scopes; return this; }
        public PlatformConnectionBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public PlatformConnectionBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public PlatformConnection build() {
            return new PlatformConnection(id, user, platform, platformUserId, accessToken, refreshToken, tokenExpiresAt, scopes, createdAt, updatedAt);
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public PlatformType getPlatform() { return platform; }
    public void setPlatform(PlatformType platform) { this.platform = platform; }
    public String getPlatformUserId() { return platformUserId; }
    public void setPlatformUserId(String platformUserId) { this.platformUserId = platformUserId; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public OffsetDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(OffsetDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
