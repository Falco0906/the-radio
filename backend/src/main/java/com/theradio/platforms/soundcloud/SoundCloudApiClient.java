package com.theradio.platforms.soundcloud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.theradio.platforms.soundcloud.dto.SoundCloudTokenResponse;
import com.theradio.platforms.soundcloud.dto.SoundCloudUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class SoundCloudApiClient {
    private static final Logger log = LoggerFactory.getLogger(SoundCloudApiClient.class);

    @Value("${soundcloud.client.id:}")
    private String clientId;

    @Value("${soundcloud.client.secret:}")
    private String clientSecret;

    @Value("${soundcloud.redirect.uri:}")
    private String redirectUri;

    @Value("${app.soundcloud.api-base-url}")
    private String apiBaseUrl;

    private final WebClient.Builder webClientBuilder;

    public SoundCloudApiClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public String getAuthorizationUrl(String state) {
        if (clientId == null || clientId.isEmpty()) {
            throw new RuntimeException("SoundCloud Client ID is not configured. Please set SOUNDCLOUD_CLIENT_ID environment variable.");
        }
        if (redirectUri == null || redirectUri.isEmpty()) {
            throw new RuntimeException("SoundCloud Redirect URI is not configured. Please set SOUNDCLOUD_REDIRECT_URI environment variable.");
        }
        
        try {
            String authUrl = String.format(
                    "https://soundcloud.com/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code&scope=non-expiring&state=%s",
                    clientId,
                    java.net.URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
                    java.net.URLEncoder.encode(state, StandardCharsets.UTF_8)
            );
            log.info("Generated SoundCloud auth URL with client_id: {}, redirect_uri: {}", clientId, redirectUri);
            log.debug("Full auth URL: {}", authUrl);
            return authUrl;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate authorization URL", e);
        }
    }

    public SoundCloudTokenResponse exchangeCodeForToken(String code) {
        WebClient webClient = webClientBuilder.baseUrl("https://api.soundcloud.com").build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("redirect_uri", redirectUri);
        formData.add("scope", "non-expiring");

        try {
            log.info("Exchanging SoundCloud authorization code for token. Client ID: {}, Code: {}, Redirect URI: {}", 
                    clientId, code.substring(0, Math.min(20, code.length())) + "...", redirectUri);
            
            SoundCloudTokenResponse response = webClient.post()
                    .uri("/oauth2/token")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(SoundCloudTokenResponse.class)
                    .block();
            
            if (response != null) {
                log.info("Successfully obtained SoundCloud access token. User ID: {}", response.getScope());
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to exchange code for token. Error: {}", e.getMessage(), e);
            return null;
        }
    }

    public SoundCloudUserInfo getUserInfo(String accessToken) {
        WebClient webClient = webClientBuilder.baseUrl(apiBaseUrl).build();

        try {
            return webClient.get()
                    .uri("/me?access_token={accessToken}", accessToken)
                    .retrieve()
                    .bodyToMono(SoundCloudUserInfo.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to get user info from SoundCloud", e);
            return null;
        }
    }

    public List<Map<String, Object>> getCurrentlyPlaying(String accessToken) {
        // SoundCloud doesn't have a direct "currently playing" endpoint
        // We'll need to get the user's recent activities or tracks
        WebClient webClient = webClientBuilder.baseUrl(apiBaseUrl).build();

        try {
            log.debug("Fetching SoundCloud tracks for access token: {}...", accessToken.substring(0, Math.min(10, accessToken.length())) + "...");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> response = webClient.get()
                    .uri("/me/favorites?access_token={accessToken}&limit=1", accessToken)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> {
                        log.error("SoundCloud API error! Status code: {}", clientResponse.statusCode());
                        return clientResponse.bodyToMono(String.class)
                                .doOnNext(body -> log.error("Error body: {}", body))
                                .map(body -> new RuntimeException("SoundCloud API error: " + body));
                    })
                    .bodyToMono(List.class)
                    .block();
            return response;
        } catch (Exception e) {
            log.error("Failed to get user activities from SoundCloud. Exception: {}", e.getMessage());
            return null;
        }
    }
}
