-- The Radio Database Schema
-- PostgreSQL

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    username VARCHAR(100) UNIQUE NOT NULL,
    display_name VARCHAR(100),
    is_live BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);

-- Platform types enum
CREATE TYPE platform_type AS ENUM ('SPOTIFY', 'APPLE_MUSIC', 'YOUTUBE_MUSIC');

-- User platform connections
CREATE TABLE user_platform_connections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform platform_type NOT NULL,
    platform_user_id VARCHAR(255) NOT NULL,
    access_token TEXT NOT NULL,
    refresh_token TEXT,
    token_expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE(user_id, platform)
);

CREATE INDEX idx_platform_connections_user_id ON user_platform_connections(user_id);
CREATE INDEX idx_platform_connections_platform ON user_platform_connections(platform);

-- Friend requests
CREATE TABLE friend_requests (
    id BIGSERIAL PRIMARY KEY,
    requester_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'PENDING' NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE(requester_id, recipient_id),
    CHECK (requester_id != recipient_id)
);

CREATE INDEX idx_friend_requests_requester ON friend_requests(requester_id);
CREATE INDEX idx_friend_requests_recipient ON friend_requests(recipient_id);
CREATE INDEX idx_friend_requests_status ON friend_requests(status);

-- Friends (bidirectional relationship)
CREATE TABLE friends (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    friend_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE(user_id, friend_id),
    CHECK (user_id != friend_id)
);

CREATE INDEX idx_friends_user_id ON friends(user_id);
CREATE INDEX idx_friends_friend_id ON friends(friend_id);

-- Listening state (current playback state per user)
CREATE TABLE listening_state (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform platform_type NOT NULL,
    track_id VARCHAR(255),
    track_name VARCHAR(500),
    artist VARCHAR(500),
    album_art_url TEXT,
    progress_ms INTEGER DEFAULT 0,
    duration_ms INTEGER,
    is_playing BOOLEAN DEFAULT FALSE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE(user_id)
);

CREATE INDEX idx_listening_state_user_id ON listening_state(user_id);
CREATE INDEX idx_listening_state_updated_at ON listening_state(updated_at);

-- Update timestamp trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply triggers
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_platform_connections_updated_at BEFORE UPDATE ON user_platform_connections
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_friend_requests_updated_at BEFORE UPDATE ON friend_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_listening_state_updated_at BEFORE UPDATE ON listening_state
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Test data (optional - for development only)
-- Password: password123 (BCrypt hash: $2a$10$slYQmyNdGzin7olVN3p5be0ysZzEeEqdGYeP36q/gxuLueFMW6YGy)
INSERT INTO users (email, username, password_hash, display_name, is_live, created_at, updated_at)
VALUES ('test@example.com', 'testuser', '$2a$10$slYQmyNdGzin7olVN3p5be0ysZzEeEqdGYeP36q/gxuLueFMW6YGy', 'Test User', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
