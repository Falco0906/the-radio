package com.theradio.auth.dto;

public class AuthResponse {
    private String token;
    private UserDto user;

    public AuthResponse() {}

    public AuthResponse(String token, UserDto user) {
        this.token = token;
        this.user = user;
    }

    public static AuthResponseBuilder builder() {
        return new AuthResponseBuilder();
    }

    public static class AuthResponseBuilder {
        private String token;
        private UserDto user;

        public AuthResponseBuilder token(String token) { this.token = token; return this; }
        public AuthResponseBuilder user(UserDto user) { this.user = user; return this; }

        public AuthResponse build() {
            return new AuthResponse(token, user);
        }
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }

    public static class UserDto {
        private Long id;
        private String email;
        private String username;
        private String displayName;
        private Boolean isLive;

        public UserDto() {}

        public UserDto(Long id, String email, String username, String displayName, Boolean isLive) {
            this.id = id;
            this.email = email;
            this.username = username;
            this.displayName = displayName;
            this.isLive = isLive;
        }

        public static UserDtoBuilder builder() {
            return new UserDtoBuilder();
        }

        public static class UserDtoBuilder {
            private Long id;
            private String email;
            private String username;
            private String displayName;
            private Boolean isLive;

            public UserDtoBuilder id(Long id) { this.id = id; return this; }
            public UserDtoBuilder email(String email) { this.email = email; return this; }
            public UserDtoBuilder username(String username) { this.username = username; return this; }
            public UserDtoBuilder displayName(String displayName) { this.displayName = displayName; return this; }
            public UserDtoBuilder isLive(Boolean isLive) { this.isLive = isLive; return this; }

            public UserDto build() {
                return new UserDto(id, email, username, displayName, isLive);
            }
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public Boolean getIsLive() { return isLive; }
        public void setIsLive(Boolean isLive) { this.isLive = isLive; }
    }
}
