package com.theradio.platforms.soundcloud.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SoundCloudTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("scope")
    private String scope;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    @JsonProperty("expires_in")
    private Long expiresIn;

    // Constructors
    public SoundCloudTokenResponse() {}

    public SoundCloudTokenResponse(String accessToken, String scope, String tokenType, Long expiresIn) {
        this.accessToken = accessToken;
        this.scope = scope;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
