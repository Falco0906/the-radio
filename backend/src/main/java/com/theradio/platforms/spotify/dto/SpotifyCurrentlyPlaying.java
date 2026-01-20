package com.theradio.platforms.spotify.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpotifyCurrentlyPlaying {
    @JsonProperty("is_playing")
    private Boolean isPlaying;

    @JsonProperty("progress_ms")
    private Integer progressMs;

    private SpotifyTrack item;

    public Boolean getIsPlaying() { return isPlaying; }
    public void setIsPlaying(Boolean isPlaying) { this.isPlaying = isPlaying; }
    public Integer getProgressMs() { return progressMs; }
    public void setProgressMs(Integer progressMs) { this.progressMs = progressMs; }
    public SpotifyTrack getItem() { return item; }
    public void setItem(SpotifyTrack item) { this.item = item; }

    public static class SpotifyTrack {
        private String id;
        private String name;
        @JsonProperty("duration_ms")
        private Integer durationMs;
        private SpotifyAlbum album;
        private SpotifyArtist[] artists;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getDurationMs() { return durationMs; }
        public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }
        public SpotifyAlbum getAlbum() { return album; }
        public void setAlbum(SpotifyAlbum album) { this.album = album; }
        public SpotifyArtist[] getArtists() { return artists; }
        public void setArtists(SpotifyArtist[] artists) { this.artists = artists; }

        public static class SpotifyAlbum {
            private SpotifyImage[] images;

            public SpotifyImage[] getImages() { return images; }
            public void setImages(SpotifyImage[] images) { this.images = images; }
        }

        public static class SpotifyImage {
            private String url;
            private Integer height;
            private Integer width;

            public String getUrl() { return url; }
            public void setUrl(String url) { this.url = url; }
            public Integer getHeight() { return height; }
            public void setHeight(Integer height) { this.height = height; }
            public Integer getWidth() { return width; }
            public void setWidth(Integer width) { this.width = width; }
        }

        public static class SpotifyArtist {
            private String name;

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
        }
    }
}
