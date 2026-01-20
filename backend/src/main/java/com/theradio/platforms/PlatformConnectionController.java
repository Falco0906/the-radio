package com.theradio.platforms;

import com.theradio.auth.AuthService;
import com.theradio.domain.model.PlatformConnection;
import com.theradio.domain.model.User;
import com.theradio.domain.repository.PlatformConnectionRepository;
import com.theradio.platforms.spotify.SpotifyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/platforms")
public class PlatformConnectionController {
    public PlatformConnectionController(SpotifyService spotifyService, PlatformConnectionRepository connectionRepository, AuthService authService) {
        this.spotifyService = spotifyService;
        this.connectionRepository = connectionRepository;
        this.authService = authService;
    }


    private final SpotifyService spotifyService;
    private final PlatformConnectionRepository connectionRepository;
    private final AuthService authService;

    @GetMapping("/connections")
    public ResponseEntity<List<PlatformConnectionDto>> getConnections() {
        User currentUser = authService.getCurrentUser();
        List<PlatformConnection> connections = connectionRepository.findByUser(currentUser);
        
        List<PlatformConnectionDto> dtos = connections.stream()
                .map(conn -> {
                    PlatformConnectionDto dto = PlatformConnectionDto.builder()
                            .id(conn.getId())
                            .platform(conn.getPlatform().name())
                            .platformUserId(conn.getPlatformUserId())
                            .connectedAt(conn.getCreatedAt())
                            .build();
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/spotify/connect")
    public ResponseEntity<Map<String, String>> initiateSpotifyConnection() {
        String authUrl = spotifyService.getAuthorizationUrl();
        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    @GetMapping("/spotify/callback")
    public org.springframework.web.servlet.ModelAndView handleSpotifyCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        
        String redirectUrl = "http://localhost:5173/connect-platform";
        
        if (error != null) {
            redirectUrl += "?error=" + java.net.URLEncoder.encode("Spotify authorization failed: " + error, java.nio.charset.StandardCharsets.UTF_8);
            return new org.springframework.web.servlet.ModelAndView("redirect:" + redirectUrl);
        }
        
        if (code == null || state == null) {
            redirectUrl += "?error=" + java.net.URLEncoder.encode("Missing authorization code or state", java.nio.charset.StandardCharsets.UTF_8);
            return new org.springframework.web.servlet.ModelAndView("redirect:" + redirectUrl);
        }
        
        try {
            spotifyService.handleCallback(code, state);
            redirectUrl += "?success=Spotify connected successfully";
        } catch (Exception e) {
            redirectUrl += "?error=" + java.net.URLEncoder.encode("Failed to connect Spotify: " + e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
        
        return new org.springframework.web.servlet.ModelAndView("redirect:" + redirectUrl);
    }

    @DeleteMapping("/connections/{connectionId}")
    public ResponseEntity<Void> disconnectPlatform(@PathVariable Long connectionId) {
        User currentUser = authService.getCurrentUser();
        PlatformConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        if (!connection.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        connectionRepository.delete(connection);
        return ResponseEntity.ok().build();
    }

    public static class PlatformConnectionDto {
        private Long id;
        private String platform;
        private String platformUserId;
        private java.time.OffsetDateTime connectedAt;

        public static PlatformConnectionDtoBuilder builder() {
            return new PlatformConnectionDtoBuilder();
        }

        public static class PlatformConnectionDtoBuilder {
            private Long id;
            private String platform;
            private String platformUserId;
            private java.time.OffsetDateTime connectedAt;

            public PlatformConnectionDtoBuilder id(Long id) { this.id = id; return this; }
            public PlatformConnectionDtoBuilder platform(String platform) { this.platform = platform; return this; }
            public PlatformConnectionDtoBuilder platformUserId(String platformUserId) { this.platformUserId = platformUserId; return this; }
            public PlatformConnectionDtoBuilder connectedAt(java.time.OffsetDateTime connectedAt) { this.connectedAt = connectedAt; return this; }

            public PlatformConnectionDto build() {
                PlatformConnectionDto dto = new PlatformConnectionDto();
                dto.id = id;
                dto.platform = platform;
                dto.platformUserId = platformUserId;
                dto.connectedAt = connectedAt;
                return dto;
            }
        }

        public Long getId() { return id; }
        public String getPlatform() { return platform; }
        public String getPlatformUserId() { return platformUserId; }
        public java.time.OffsetDateTime getConnectedAt() { return connectedAt; }
    }
}

