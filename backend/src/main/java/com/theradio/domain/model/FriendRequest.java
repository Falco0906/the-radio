package com.theradio.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "friend_requests",
       uniqueConstraints = @UniqueConstraint(columnNames = {"requester_id", "recipient_id"}))
public class FriendRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FriendRequestStatus status = FriendRequestStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public FriendRequest() {}

    public FriendRequest(Long id, User requester, User recipient, FriendRequestStatus status, 
                        OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.requester = requester;
        this.recipient = recipient;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static FriendRequestBuilder builder() {
        return new FriendRequestBuilder();
    }

    public static class FriendRequestBuilder {
        private Long id;
        private User requester;
        private User recipient;
        private FriendRequestStatus status = FriendRequestStatus.PENDING;
        private OffsetDateTime createdAt = OffsetDateTime.now();
        private OffsetDateTime updatedAt = OffsetDateTime.now();

        public FriendRequestBuilder id(Long id) { this.id = id; return this; }
        public FriendRequestBuilder requester(User requester) { this.requester = requester; return this; }
        public FriendRequestBuilder recipient(User recipient) { this.recipient = recipient; return this; }
        public FriendRequestBuilder status(FriendRequestStatus status) { this.status = status; return this; }
        public FriendRequestBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public FriendRequestBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public FriendRequest build() {
            return new FriendRequest(id, requester, recipient, status, createdAt, updatedAt);
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getRequester() { return requester; }
    public void setRequester(User requester) { this.requester = requester; }
    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }
    public FriendRequestStatus getStatus() { return status; }
    public void setStatus(FriendRequestStatus status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
