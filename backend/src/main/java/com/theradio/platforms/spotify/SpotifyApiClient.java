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
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class SpotifyApiClient {
    private static final Logger log = LoggerFactory.getLogger(com.theradio.platforms.spotify.SpotifyApiClient.class);


    @Value("${spring.security.oauth2.client.registration.spotify.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.spotify.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.spotify.redirect-uri}")
    private String redirectUri;

    @Value("${app.spotify.api-base-url}")
    private String apiBaseUrl;

    private final WebClient.Builder webClientBuilder;

    public SpotifyApiClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public String getAuthorizationUrl(String state) {
        if (clientId == null || clientId.isEmpty()) {
            throw new RuntimeException("Spotify Client ID is not configured. Please set SPOTIFY_CLIENT_ID environment variable.");
        }
        if (redirectUri == null || redirectUri.isEmpty()) {
            throw new RuntimeException("Spotify Redirect URI is not configured. Please set SPOTIFY_REDIRECT_URI environment variable.");
        }
        String scope = "user-read-currently-playing user-read-playback-state";
        try {
            return String.format(
                    "https://accounts.spotify.com/authorize?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&state=%s",
                    clientId,
                    java.net.URLEncoder.encode(redirectUri, java.nio.charset.StandardCharsets.UTF_8),
                    java.net.URLEncoder.encode(scope, java.nio.charset.StandardCharsets.UTF_8),
                    java.net.URLEncoder.encode(state, java.nio.charset.StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate authorization URL", e);
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

    public SpotifyCurrentlyPlaying getCurrentlyPlaying(String accessToken) {
        WebClient webClient = webClientBuilder.baseUrl(apiBaseUrl).build();

        try {
            return webClient.get()
                    .uri("/me/player/currently-playing")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(SpotifyCurrentlyPlaying.class)
                    .block();
        } catch (Exception e) {
            log.debug("No currently playing track or error: {}", e.getMessage());
            return null;
        }
    }
}

