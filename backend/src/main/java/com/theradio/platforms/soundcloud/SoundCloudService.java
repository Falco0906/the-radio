package com.theradio.platforms.soundcloud;

import com.theradio.domain.model.PlatformConnection;
import com.theradio.domain.model.PlatformType;
import com.theradio.domain.model.User;
import com.theradio.domain.repository.PlatformConnectionRepository;
import com.theradio.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.annotation.PostConstruct;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;
import java.util.Map;

@Service
public class SoundCloudService {
    private static final Logger log = LoggerFactory.getLogger(SoundCloudService.class);

    @Value("${soundcloud.client.id:}")
    private String clientId;

    @Value("${soundcloud.client.secret:}")
    private String clientSecret;

    @Value("${soundcloud.redirect.uri:}")
    private String redirectUri;

    @Value("${app.jwt.secret}")
    private String stateSigningSecret;

    private final WebClient.Builder webClientBuilder;
    private final PlatformConnectionRepository connectionRepository;
    private final UserRepository userRepository;

    public SoundCloudService(WebClient.Builder webClientBuilder,
            PlatformConnectionRepository connectionRepository,
            UserRepository userRepository) {
        this.webClientBuilder = webClientBuilder;
        this.connectionRepository = connectionRepository;
        this.userRepository = userRepository;
    }

    public Object getMe(String accessToken) {
        try {
            if (accessToken != null && !accessToken.isBlank()) {
                String prefix = accessToken.length() >= 8 ? accessToken.substring(0, 8) : accessToken;
                log.info("Calling SC /me with token prefix: {}", prefix);
            }
            WebClient webClient = webClientBuilder.baseUrl("https://api.soundcloud.com").build();
            return webClient.get()
                .uri("/me")
                .header(HttpHeaders.AUTHORIZATION, "OAuth " + accessToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class).flatMap(body -> {
                    log.error("SoundCloud /me failed: HTTP {} - {}", r.statusCode().value(), body);
                    return r.createException();
                }))
                .bodyToMono(Object.class)
                .block();
        } catch (WebClientResponseException e) {
            log.error("SoundCloud /me failed: HTTP {} - {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw e;
        }
    }

    @PostConstruct
    public void logSoundCloudConfig() {
        log.info("Loaded SoundCloud client id: {}", clientId);
    }

    /**
     * Build the SoundCloud authorization URL
     */
    public String buildAuthorizationUrl(String state) {
        log.info("Building SoundCloud authorization URL");
        
        if (clientId == null || clientId.isEmpty()) {
            throw new RuntimeException("SoundCloud Client ID not configured. Check soundcloud.client.id");
        }
        if (redirectUri == null || redirectUri.isEmpty()) {
            throw new RuntimeException("SoundCloud Redirect URI not configured. Check soundcloud.redirect.uri");
        }
        if (state == null || state.isBlank()) {
            throw new RuntimeException("OAuth state is required");
        }

        String authUrl = String.format(
            "https://soundcloud.com/connect?" +
            "client_id=%s&" +
            "redirect_uri=%s&" +
            "response_type=code&" +
            "scope=non-expiring&" +
            "state=%s",
            clientId,
            urlEncode(redirectUri),
            urlEncode(state)
        );

        log.debug("Authorization URL: {}", authUrl);
        return authUrl;
    }

    /**
     * Exchange authorization code for access token
     */
    public String exchangeCodeForToken(String code) {
        log.info("Exchanging authorization code for access token");
        
        if (code == null || code.isEmpty()) {
            throw new RuntimeException("Authorization code is missing");
        }

        if (clientId == null || clientId.isEmpty() || clientSecret == null || clientSecret.isEmpty()) {
            throw new RuntimeException("SoundCloud client credentials not configured");
        }

        try {
            WebClient webClient = webClientBuilder.baseUrl("https://api.soundcloud.com").build();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                .uri("/oauth2/token")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .body(BodyInserters.fromFormData("client_id", clientId)
                    .with("client_secret", clientSecret)
                    .with("grant_type", "authorization_code")
                    .with("redirect_uri", redirectUri)
                    .with("code", code)
                    .with("scope", "non-expiring"))
                .retrieve()
                .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class).flatMap(body -> {
                    log.error("SoundCloud token exchange failed: HTTP {} - {}", r.statusCode().value(), body);
                    return r.createException();
                }))
                .bodyToMono(Map.class)
                .block();

            String accessToken = response == null ? null : (String) response.get("access_token");
            if (accessToken == null || accessToken.isEmpty()) {
                log.error("SoundCloud token exchange failed: missing access_token");
                throw new RuntimeException("Invalid token response");
            }

            log.info("Token exchange success");
            return accessToken;

        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("SoundCloud token exchange failed: HTTP {} - {}", e.getStatusCode().value(), responseBody);
            throw new RuntimeException("Token exchange failed: HTTP " + e.getStatusCode().value(), e);
        } catch (Exception e) {
            log.error("SoundCloud token exchange failed: {}", e.getMessage(), e);
            throw new RuntimeException("Token exchange failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void storeTokenForUserId(Long userId, String soundCloudUserId, String accessToken, String refreshToken, Object expiresIn) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Storing SoundCloud token for userId: {}", userId);

        Long expiresInSeconds = null;
        if (expiresIn instanceof Number) {
            expiresInSeconds = ((Number) expiresIn).longValue();
        }

        OffsetDateTime expiresAt = expiresInSeconds != null
            ? OffsetDateTime.now().plusSeconds(expiresInSeconds)
            : null;

        PlatformConnection existing = connectionRepository
            .findByUserAndPlatform(user, PlatformType.SOUNDCLOUD)
            .orElse(null);

        if (existing != null) {
            log.info("Updating existing SoundCloud connection for userId={}", userId);
            existing.setAccessToken(accessToken);
            existing.setRefreshToken(refreshToken);
            existing.setTokenExpiresAt(expiresAt);
            existing.setUpdatedAt(OffsetDateTime.now());
            connectionRepository.save(existing);
        } else {
            log.info("Creating new SoundCloud connection for userId={}", userId);
            PlatformConnection connection = PlatformConnection.builder()
                .user(user)
                .platform(PlatformType.SOUNDCLOUD)
                .platformUserId(soundCloudUserId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenExpiresAt(expiresAt)
                .build();
            connectionRepository.save(connection);
        }

        log.info("Saved SoundCloud token for userId={}", userId);
    }

    /**
     * Get user's current SoundCloud connection
     */
    public PlatformConnection getConnectionForUserId(Long userId) {
        return connectionRepository
            .findByUserIdAndPlatform(userId, PlatformType.SOUNDCLOUD)
            .orElse(null);
    }

    /**
     * URL encode a string
     */
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            throw new RuntimeException("URL encoding failed", e);
        }
    }

    private String base64UrlEncode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC signing failed", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
