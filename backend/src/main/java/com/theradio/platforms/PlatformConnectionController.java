package com.theradio.platforms;

import com.theradio.auth.AuthService;
import com.theradio.domain.model.PlatformConnection;
import com.theradio.domain.model.User;
import com.theradio.domain.repository.ListeningStateRepository;
import com.theradio.domain.repository.PlatformConnectionRepository;
import com.theradio.platforms.soundcloud.SoundCloudService;
import com.theradio.platforms.spotify.SpotifyService;
import com.theradio.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.time.Duration;

@RestController
@RequestMapping("/api/platforms")
public class PlatformConnectionController {
    private static final Logger log = LoggerFactory.getLogger(PlatformConnectionController.class);

    private final SoundCloudService soundCloudService;
    private final SpotifyService spotifyService;
    private final PlatformConnectionRepository connectionRepository;
    private final ListeningStateRepository listeningStateRepository;
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public PlatformConnectionController(SoundCloudService soundCloudService,
            SpotifyService spotifyService,
            PlatformConnectionRepository connectionRepository,
            ListeningStateRepository listeningStateRepository,
            AuthService authService,
            JwtTokenProvider jwtTokenProvider) {
        this.soundCloudService = soundCloudService;
        this.spotifyService = spotifyService;
        this.connectionRepository = connectionRepository;
        this.listeningStateRepository = listeningStateRepository;
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/connections")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PlatformConnectionDto>> getConnections() {
        User currentUser = authService.getCurrentUser();
        List<PlatformConnection> connections = connectionRepository.findByUser(currentUser);

        List<PlatformConnectionDto> dtos = connections.stream()
            .map(conn -> new PlatformConnectionDto(
                conn.getId(),
                conn.getPlatform().name(),
                conn.getPlatformUserId(),
                conn.getCreatedAt()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/connections/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> getConnectionsStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.theradio.security.UserPrincipal userPrincipal) {
            userId = userPrincipal.getId();
        }

        if (userId == null) {
            User currentUser = authService.getCurrentUser();
            userId = currentUser.getId();
        }

        boolean soundcloudConnected = connectionRepository.existsByUserIdAndPlatform(
            userId,
            com.theradio.domain.model.PlatformType.SOUNDCLOUD
        );
        boolean spotifyConnected = connectionRepository.existsByUserIdAndPlatform(
            userId,
            com.theradio.domain.model.PlatformType.SPOTIFY
        );
        Map<String, Boolean> status = new HashMap<>();
        status.put("soundcloud", soundcloudConnected);
        status.put("spotify", spotifyConnected);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/soundcloud/connect")
    public ResponseEntity<?> connectSoundCloud(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = jwtTokenProvider.getUserIdFromJWT(token);
        log.info("CONNECT SOUNDCLOUD userId={}", userId);

        String state = jwtTokenProvider.generateSoundCloudStateToken(userId, Duration.ofMinutes(5));
        String url = soundCloudService.buildAuthorizationUrl(state);

        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/spotify/connect")
    public ResponseEntity<?> connectSpotify(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = jwtTokenProvider.getUserIdFromJWT(token);
        log.info("CONNECT SPOTIFY request for userId={}", userId);

        try {
            String url = spotifyService.initiateConnect(userId);
            
            if (url == null || url.isBlank()) {
                log.error("Spotify service returned null/blank URL for userId={}", userId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Spotify connection failed: Generated URL is empty"));
            }

            return ResponseEntity.ok(Map.of("authorizationUrl", url));
        } catch (Exception e) {
            log.error("Spotify connection initiation failed for userId={}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Spotify connection failed: " + e.getMessage()));
        }
    }

    @GetMapping("/spotify/callback")
    public ResponseEntity<Void> handleSpotifyCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        log.info("Spotify OAuth callback hit. Code: {}, State: {}, Error: {}", 
                code != null ? "PRESENT" : "MISSING", 
                state != null ? state : "MISSING", 
                error != null ? error : "NONE");

        if (error != null) {
            log.warn("Spotify returned error param: {}", error);
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/connect-platform?error=spotify_" + error))
                .build();
        }

        if (state == null || state.isBlank()) {
            log.warn("Spotify callback missing state parameter");
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/connect-platform?error=missing_state"))
                .build();
        }

        if (code == null || code.isBlank()) {
            log.warn("Spotify callback missing code parameter");
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/connect-platform?error=missing_code"))
                .build();
        }

        Long userId;
        try {
            userId = spotifyService.validateState(state);
        } catch (Exception e) {
            log.warn("Spotify state validation failed: {}", e.getMessage());
            String errorType = e.getMessage().contains("expired") ? "state_expired" : "state_mismatch";
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/connect-platform?error=" + errorType))
                .build();
        }

        try {
            spotifyService.handleCallback(userId, code);
            log.info("Spotify connected successfully for userId={}", userId);
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/connect-platform?connected=spotify"))
                .build();
        } catch (Exception e) {
            log.error("Spotify token exchange failed for userId={}: {}", userId, e.getMessage());
            String errorParam = e.getMessage(); // handleCallback throws specific keys
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/connect-platform?error=" + errorParam))
                .build();
        }
    }

    @GetMapping("/soundcloud/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSoundCloudMe() {
        User currentUser = authService.getCurrentUser();
        PlatformConnection connection = connectionRepository
            .findByUserAndPlatform(currentUser, com.theradio.domain.model.PlatformType.SOUNDCLOUD)
            .orElse(null);

        if (connection == null || connection.getAccessToken() == null || connection.getAccessToken().isBlank()) {
            return ResponseEntity.badRequest().body("SoundCloud not connected");
        }

        try {
            Object me = soundCloudService.getMe(connection.getAccessToken());
            return ResponseEntity.ok(me);
        } catch (Exception e) {
            log.error("SoundCloud /me request failed", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("SoundCloud API failed");
        }
    }

    @GetMapping("/soundcloud/callback")
    public ResponseEntity<Void> handleSoundCloudCallback(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(required = false) String error) {

        log.info("OAuth callback received");

        log.info("State received: {}", state);

        Long userId;
        try {
            userId = jwtTokenProvider.getUserIdFromSoundCloudStateToken(state);
        } catch (Exception e) {
            log.warn("Invalid OAuth state token during SoundCloud callback", e);
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/connect-platform?error=invalid_state"))
                .build();
        }

        log.info("OAuth callback: decoded userId={} from state", userId);

        if (error != null) {
            log.warn("SoundCloud authorization denied by user: {}", error);
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/connect-platform?error=soundcloud_auth_failed"))
                .build();
        }

        log.info("Exchanging SoundCloud authorization code for userId={}", userId);

        try {
            String accessToken = soundCloudService.exchangeCodeForToken(code);

            // Fetch real SoundCloud user ID from /me before storing
            String soundCloudUserId;
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> meResponse = (Map<String, Object>) soundCloudService.getMe(accessToken);
                Object idObj = meResponse != null ? meResponse.get("id") : null;
                soundCloudUserId = idObj != null ? String.valueOf(idObj) : "unknown";
                log.info("SoundCloud /me returned userId={}", soundCloudUserId);
            } catch (Exception meEx) {
                log.warn("Could not fetch SoundCloud user ID from /me, using fallback", meEx);
                soundCloudUserId = "sc_user_" + userId;
            }

            soundCloudService.storeTokenForUserId(userId, soundCloudUserId, accessToken, null, null);
            log.info("SoundCloud connected successfully for appUserId={}, scUserId={}", userId, soundCloudUserId);
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/connect-platform?connected=soundcloud"))
                .build();
        } catch (Exception e) {
            log.error("SoundCloud token exchange failed for userId={}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/connect-platform?error=soundcloud_auth_failed"))
                .build();
        }
    }

    @DeleteMapping("/connections/{connectionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> disconnectPlatform(@PathVariable Long connectionId) {
        User currentUser = authService.getCurrentUser();
        PlatformConnection connection = connectionRepository.findById(connectionId)
            .orElseThrow(() -> new RuntimeException("Connection not found"));

        if (!connection.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        connectionRepository.delete(connection);

        // Also clear listening state if it belongs to this platform
        listeningStateRepository.findByUser(currentUser).ifPresent(state -> {
            if (state.getPlatform() == connection.getPlatform()) {
                listeningStateRepository.delete(state);
            }
        });

        return ResponseEntity.ok().build();
    }

    public static class PlatformConnectionDto {
        private final Long id;
        private final String platform;
        private final String platformUserId;
        private final java.time.OffsetDateTime connectedAt;

        public PlatformConnectionDto(Long id, String platform, String platformUserId, java.time.OffsetDateTime connectedAt) {
            this.id = id;
            this.platform = platform;
            this.platformUserId = platformUserId;
            this.connectedAt = connectedAt;
        }

        public Long getId() { return id; }
        public String getPlatform() { return platform; }
        public String getPlatformUserId() { return platformUserId; }
        public java.time.OffsetDateTime getConnectedAt() { return connectedAt; }
    }
}
