package com.theradio.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "friends",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "friend_id"}))
public class Friend {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id", nullable = false)
    private User friend;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Friend() {}

    public Friend(Long id, User user, User friend, OffsetDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.friend = friend;
        this.createdAt = createdAt;
    }

    public static FriendBuilder builder() {
        return new FriendBuilder();
    }

    public static class FriendBuilder {
        private Long id;
        private User user;
        private User friend;
        private OffsetDateTime createdAt = OffsetDateTime.now();

        public FriendBuilder id(Long id) { this.id = id; return this; }
        public FriendBuilder user(User user) { this.user = user; return this; }
        public FriendBuilder friend(User friend) { this.friend = friend; return this; }
        public FriendBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Friend build() {
            return new Friend(id, user, friend, createdAt);
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public User getFriend() { return friend; }
    public void setFriend(User friend) { this.friend = friend; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
