package com.theradio.platforms.spotify.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpotifyUserInfo {
    private String id;
    private String email;
    @JsonProperty("display_name")
    private String displayName;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
