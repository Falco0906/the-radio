package com.theradio.platforms.spotify;

import com.theradio.auth.AuthService;
import com.theradio.domain.model.PlatformConnection;
import com.theradio.domain.model.PlatformType;
import com.theradio.domain.model.User;
import com.theradio.domain.repository.PlatformConnectionRepository;
import com.theradio.domain.repository.UserRepository;
import com.theradio.platforms.spotify.dto.SpotifyTokenResponse;
import com.theradio.platforms.spotify.dto.SpotifyUserInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class SpotifyService {
    public SpotifyService(SpotifyApiClient apiClient, PlatformConnectionRepository connectionRepository, AuthService authService, UserRepository userRepository) {
        this.apiClient = apiClient;
        this.connectionRepository = connectionRepository;
        this.authService = authService;
        this.userRepository = userRepository;
    }


    private final SpotifyApiClient apiClient;
    private final PlatformConnectionRepository connectionRepository;
    private final AuthService authService;
    private final UserRepository userRepository;

    public String getAuthorizationUrl(String state) {
        return apiClient.getAuthorizationUrl(state);
    }

    @Transactional
    public void handleCallback(Long userId, String code) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SpotifyTokenResponse tokenResponse = apiClient.exchangeCodeForToken(code);
        if (tokenResponse == null) {
            throw new RuntimeException("Failed to exchange code for token");
        }

        SpotifyUserInfo userInfo = apiClient.getUserInfo(tokenResponse.getAccessToken());
        if (userInfo == null) {
            throw new RuntimeException("Failed to get user info");
        }

        // Check if connection already exists
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
            connectionRepository.save(existing);
        } else {
            PlatformConnection connection = PlatformConnection.builder()
                    .user(currentUser)
                    .platform(PlatformType.SPOTIFY)
                    .platformUserId(userInfo.getId())
                    .accessToken(tokenResponse.getAccessToken())
                    .refreshToken(tokenResponse.getRefreshToken())
                    .tokenExpiresAt(expiresAt)
                    .build();
            connectionRepository.save(connection);
        }
    }

    public String refreshAccessToken(PlatformConnection connection) {
        if (connection.getRefreshToken() == null) {
            throw new RuntimeException("No refresh token available");
        }

        SpotifyTokenResponse tokenResponse = apiClient.refreshToken(connection.getRefreshToken());
        if (tokenResponse == null) {
            throw new RuntimeException("Failed to refresh token");
        }

        OffsetDateTime expiresAt = tokenResponse.getExpiresIn() != null
                ? OffsetDateTime.now().plusSeconds(tokenResponse.getExpiresIn())
                : null;

        connection.setAccessToken(tokenResponse.getAccessToken());
        if (tokenResponse.getRefreshToken() != null) {
            connection.setRefreshToken(tokenResponse.getRefreshToken());
        }
        connection.setTokenExpiresAt(expiresAt);
        connectionRepository.save(connection);

        return connection.getAccessToken();
    }

    public String getValidAccessToken(PlatformConnection connection) {
        if (connection.getTokenExpiresAt() == null || 
            connection.getTokenExpiresAt().isAfter(OffsetDateTime.now().plusMinutes(5))) {
            return connection.getAccessToken();
        }

        return refreshAccessToken(connection);
    }
}


