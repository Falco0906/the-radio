package com.theradio.platforms.spotify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.theradio.platforms.spotify.dto.SpotifyCurrentlyPlaying;
import com.theradio.platforms.spotify.dto.SpotifyTokenResponse;
import com.theradio.platforms.spotify.dto.SpotifyUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class SpotifyApiClient {
    private static final Logger log = LoggerFactory.getLogger(com.theradio.platforms.spotify.SpotifyApiClient.class);


    @Value("${app.spotify.client-id}")
    private String clientId;

    @Value("${app.spotify.client-secret}")
    private String clientSecret;

    @Value("${app.spotify.redirect-uri}")
    private String redirectUri;

    @Value("${app.spotify.api-base-url}")
    private String apiBaseUrl;

    private final WebClient.Builder webClientBuilder;

    public SpotifyApiClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @jakarta.annotation.PostConstruct
    public void validateConfig() {
        log.info("Validating Spotify Configuration...");
        log.info("SPOTIFY_CLIENT_ID: {}", clientId != null ? (clientId.equals("dummy") ? "NOT_CONFIGURED (dummy)" : "PRESENT") : "NULL");
        log.info("SPOTIFY_REDIRECT_URI: {}", redirectUri);

        if (clientId == null || clientId.isEmpty() || clientId.equals("dummy") ||
            clientSecret == null || clientSecret.isEmpty() || clientSecret.equals("dummy")) {
            throw new IllegalStateException("Spotify credentials not configured in environment variables");
        }
    }

    public String getAuthorizationUrl(String state) {
        try {
            log.info("Generating Spotify Authorization URI...");
            log.info("Using CLIENT_ID: {}", clientId);
            log.info("Using REDIRECT_URI: {}", redirectUri);

            String scope = "user-read-currently-playing user-read-playback-state";
            String url = String.format(
                    "https://accounts.spotify.com/authorize?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&state=%s",
                    clientId,
                    java.net.URLEncoder.encode(redirectUri, java.nio.charset.StandardCharsets.UTF_8),
                    java.net.URLEncoder.encode(scope, java.nio.charset.StandardCharsets.UTF_8),
                    java.net.URLEncoder.encode(state, java.nio.charset.StandardCharsets.UTF_8)
            );

            log.info("Generated Authorization URI: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Failed to generate Spotify authorization URL: {}", e.getMessage(), e);
            throw e;
        }
    }

    public SpotifyTokenResponse exchangeCodeForToken(String code) {
        WebClient webClient = webClientBuilder.baseUrl("https://accounts.spotify.com").build();

        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("redirect_uri", redirectUri);

        return webClient.post()
                .uri("/api/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(SpotifyTokenResponse.class)
                .block();
    }

    public SpotifyTokenResponse refreshToken(String refreshToken) {
        WebClient webClient = webClientBuilder.baseUrl("https://accounts.spotify.com").build();

        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", refreshToken);

        return webClient.post()
                .uri("/api/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(SpotifyTokenResponse.class)
                .block();
    }

    public SpotifyUserInfo getUserInfo(String accessToken) {
        WebClient webClient = webClientBuilder.baseUrl(apiBaseUrl).build();

        return webClient.get()
                .uri("/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(SpotifyUserInfo.class)
                .block();
    }

    public ResponseEntity<String> getCurrentlyPlaying(String accessToken) {
        WebClient webClient = webClientBuilder.baseUrl(apiBaseUrl).build();

        try {
            return webClient.get()
                    .uri("/me/player/currently-playing")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toEntity(String.class)
                    .block();
        } catch (Exception e) {
            log.error("Error calling Spotify currently-playing API: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    public ResponseEntity<String> getPlaybackState(String accessToken) {
        WebClient webClient = webClientBuilder.baseUrl(apiBaseUrl).build();

        try {
            return webClient.get()
                    .uri("/me/player")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toEntity(String.class)
                    .block();
        } catch (Exception e) {
            log.error("Error calling Spotify playback-state API: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}

