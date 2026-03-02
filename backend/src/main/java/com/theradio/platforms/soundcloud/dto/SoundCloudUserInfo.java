package com.theradio.platforms.soundcloud.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SoundCloudUserInfo {
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("full_name")
    private String fullName;
    
    @JsonProperty("avatar_url")
    private String avatarUrl;
    
    @JsonProperty("country")
    private String country;

    // Constructors
    public SoundCloudUserInfo() {}

    public SoundCloudUserInfo(Long id, String username, String fullName, String avatarUrl, String country) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.country = country;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
