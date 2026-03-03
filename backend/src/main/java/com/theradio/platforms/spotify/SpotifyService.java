package com.theradio.platforms.spotify;

import com.theradio.auth.AuthService;
import com.theradio.domain.model.PlatformConnection;
import com.theradio.domain.model.PlatformType;
import com.theradio.domain.model.User;
import com.theradio.domain.repository.PlatformConnectionRepository;
import com.theradio.domain.repository.UserRepository;
import com.theradio.domain.repository.SpotifyAuthStateRepository;
import com.theradio.domain.model.SpotifyAuthState;
import com.theradio.platforms.spotify.dto.SpotifyTokenResponse;
import com.theradio.platforms.spotify.dto.SpotifyUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class SpotifyService {
    private static final Logger log = LoggerFactory.getLogger(SpotifyService.class);

    public SpotifyService(SpotifyApiClient apiClient, 
                          PlatformConnectionRepository connectionRepository, 
                          AuthService authService, 
                          UserRepository userRepository,
                          SpotifyAuthStateRepository authStateRepository) {
        this.apiClient = apiClient;
        this.connectionRepository = connectionRepository;
        this.authService = authService;
        this.userRepository = userRepository;
        this.authStateRepository = authStateRepository;
    }


    private final SpotifyApiClient apiClient;
    private final PlatformConnectionRepository connectionRepository;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final SpotifyAuthStateRepository authStateRepository;

    @org.springframework.beans.factory.annotation.Value("${app.spotify.client-id}")
    private String clientId;

    @org.springframework.beans.factory.annotation.Value("${app.spotify.client-secret}")
    private String clientSecret;

    @org.springframework.beans.factory.annotation.Value("${app.spotify.redirect-uri}")
    private String redirectUri;

    @jakarta.annotation.PostConstruct
    public void validateConfig() {
        log.info("Validating Spotify credentials in SpotifyService...");
        log.info("SPOTIFY_CLIENT_ID: {}", clientId != null && !clientId.equals("dummy") ? "PRESENT" : "MISSING/DUMMY");
        log.info("SPOTIFY_REDIRECT_URI: {}", redirectUri);

        if (clientId == null || clientId.isEmpty() || clientId.equals("dummy") ||
            clientSecret == null || clientSecret.isEmpty() || clientSecret.equals("dummy")) {
            log.error("Spotify credentials are NOT configured correctly!");
        }
    }

    @Transactional
    public String initiateConnect(Long userId) {
        log.info("Initiating Spotify connection for userId: {}", userId);
        
        String state = UUID.randomUUID().toString();
        SpotifyAuthState authState = SpotifyAuthState.builder()
                .state(state)
                .userId(userId)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();
        
        authStateRepository.save(authState);
        log.info("Saved Spotify auth state to DB for userId: {}", userId);

        return apiClient.getAuthorizationUrl(state);
    }

    @Transactional
    public Long validateState(String state) {
        log.info("Validating Spotify auth state: {}", state);
        
        SpotifyAuthState authState = authStateRepository.findByState(state)
                .orElseThrow(() -> new RuntimeException("state_mismatch"));

        if (authState.getExpiresAt().isBefore(OffsetDateTime.now())) {
            authStateRepository.delete(authState);
            throw new RuntimeException("state_expired");
        }

        Long userId = authState.getUserId();
        authStateRepository.delete(authState);
        log.info("Validated and deleted Spotify auth state for userId: {}", userId);
        
        return userId;
    }

    public String connect(String state) {
        log.info("Initiating Spotify connection URL generation...");
        log.info("State: {}", state);
        log.info("ClientID: {}", clientId);
        log.info("RedirectURI: {}", redirectUri);

        if (clientId == null || clientId.isEmpty() || clientId.equals("dummy")) {
            throw new IllegalStateException("Spotify environment variables not configured (client-id is missing or dummy)");
        }

        try {
            String url = apiClient.getAuthorizationUrl(state);
            if (url == null || url.isBlank()) {
                log.error("SpotifyApiClient returned null or blank URL for state: {}", state);
                throw new IllegalStateException("Failed to generate Spotify authorization URL");
            }
            log.info("Successfully generated Spotify Auth URL: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Failed to get Spotify authorization URL: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String getAuthorizationUrl(String state) {
        return connect(state);
    }

    public SpotifyTokenResponse exchangeCodeForToken(String code) {
        log.info("Delegating to API client for Spotify token exchange");
        return apiClient.exchangeCodeForToken(code);
    }

    public SpotifyUserInfo fetchUserInfo(String accessToken) {
        log.info("Delegating to API client to fetch Spotify user info");
        return apiClient.getUserInfo(accessToken);
    }

    @Transactional
    public void saveConnection(Long userId, SpotifyTokenResponse tokenResponse, SpotifyUserInfo userInfo) {
        log.info("Starting database save for Spotify connection. User={}, SpotifyId={}", userId, userInfo.getId());
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("user_not_found"));

        PlatformConnection existing = connectionRepository
                .findByUserAndPlatform(currentUser, PlatformType.SPOTIFY)
                .orElse(null);

        OffsetDateTime expiresAt = tokenResponse.getExpiresIn() != null
                ? OffsetDateTime.now().plusSeconds(tokenResponse.getExpiresIn())
                : null;

        if (existing != null) {
            existing.setAccessToken(tokenResponse.getAccessToken());
            existing.setRefreshToken(tokenResponse.getRefreshToken());
            existing.setTokenExpiresAt(expiresAt);
            existing.setPlatformUserId(userInfo.getId());
            existing.setScopes(tokenResponse.getScope());
            connectionRepository.save(existing);
            log.info("Updated existing Spotify connection for user: {}", userId);
        } else {
            PlatformConnection connection = PlatformConnection.builder()
                    .user(currentUser)
                    .platform(PlatformType.SPOTIFY)
                    .platformUserId(userInfo.getId())
                    .accessToken(tokenResponse.getAccessToken())
                    .refreshToken(tokenResponse.getRefreshToken())
                    .tokenExpiresAt(expiresAt)
                    .scopes(tokenResponse.getScope())
                    .build();
            connectionRepository.save(connection);
            log.info("Created new Spotify connection for user: {}", userId);
        }
    }

    public String refreshAccessToken(PlatformConnection connection) {
        if (connection.getRefreshToken() == null) {
            throw new RuntimeException("No refresh token available");
        }

        log.info("Refreshing Spotify access token for user {}", connection.getUser().getId());
        SpotifyTokenResponse tokenResponse = apiClient.refreshToken(connection.getRefreshToken());
        if (tokenResponse == null) {
            log.error("Failed to refresh Spotify token for user {}", connection.getUser().getId());
            throw new RuntimeException("Failed to refresh token");
        }

        OffsetDateTime expiresAt = tokenResponse.getExpiresIn() != null
                ? OffsetDateTime.now().plusSeconds(tokenResponse.getExpiresIn())
                : null;

        connection.setAccessToken(tokenResponse.getAccessToken());
        if (tokenResponse.getRefreshToken() != null) {
            connection.setRefreshToken(tokenResponse.getRefreshToken());
        }
        if (tokenResponse.getScope() != null) {
            connection.setScopes(tokenResponse.getScope());
        }
        connection.setTokenExpiresAt(expiresAt);
        connectionRepository.save(connection);

        log.info("Successfully refreshed Spotify token. New expiry: {}", expiresAt);
        return connection.getAccessToken();
    }

    public String getValidAccessToken(PlatformConnection connection) {
        log.info("Checking Spotify token validity for user {}. Expires at: {}", 
                connection.getUser().getId(), connection.getTokenExpiresAt());
        
        if (connection.getTokenExpiresAt() == null || 
            connection.getTokenExpiresAt().isAfter(OffsetDateTime.now().plusMinutes(5))) {
            return connection.getAccessToken();
        }

        log.info("Spotify token expired or expiring soon. Triggering refresh.");
        return refreshAccessToken(connection);
    }
}


