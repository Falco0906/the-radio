package com.theradio.friends;

import com.theradio.auth.AuthService;
import com.theradio.domain.model.Friend;
import com.theradio.domain.model.FriendRequest;
import com.theradio.domain.model.FriendRequestStatus;
import com.theradio.domain.model.User;
import com.theradio.domain.repository.FriendRepository;
import com.theradio.domain.repository.FriendRequestRepository;
import com.theradio.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FriendsService {
    public FriendsService(FriendRepository friendRepository, FriendRequestRepository friendRequestRepository, UserRepository userRepository, AuthService authService) {
        this.friendRepository = friendRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.userRepository = userRepository;
        this.authService = authService;
    }


    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Transactional
    public FriendRequest sendFriendRequest(Long recipientId) {
        User requester = authService.getCurrentUser();
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (requester.getId().equals(recipientId)) {
            throw new RuntimeException("Cannot send friend request to yourself");
        }

        // Check if already friends
        if (friendRepository.existsByUserAndFriend(requester, recipient) ||
            friendRepository.existsByUserAndFriend(recipient, requester)) {
            throw new RuntimeException("Already friends");
        }

        // Check if request already exists
        if (friendRequestRepository.existsByRequesterAndRecipient(requester, recipient) ||
            friendRequestRepository.existsByRequesterAndRecipient(recipient, requester)) {
            throw new RuntimeException("Friend request already exists");
        }

        FriendRequest request = FriendRequest.builder()
                .requester(requester)
                .recipient(recipient)
                .status(FriendRequestStatus.PENDING)
                .build();

        return friendRequestRepository.save(request);
    }

    @Transactional
    public void respondToFriendRequest(Long requestId, boolean accept) {
        User currentUser = authService.getCurrentUser();
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (!request.getRecipient().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new RuntimeException("Friend request already processed");
        }

        if (accept) {
            request.setStatus(FriendRequestStatus.ACCEPTED);
            friendRequestRepository.save(request);

            // Create bidirectional friendship
            Friend friendship1 = Friend.builder()
                    .user(request.getRequester())
                    .friend(request.getRecipient())
                    .build();
            Friend friendship2 = Friend.builder()
                    .user(request.getRecipient())
                    .friend(request.getRequester())
                    .build();

            friendRepository.save(friendship1);
            friendRepository.save(friendship2);
        } else {
            request.setStatus(FriendRequestStatus.REJECTED);
            friendRequestRepository.save(request);
        }
    }

    public List<FriendDto> getFriends() {
        User currentUser = authService.getCurrentUser();
        List<Friend> friendships = friendRepository.findByUser(currentUser);
        return friendships.stream()
                .map(friendship -> {
                    User friend = friendship.getFriend();
                    return FriendDto.builder()
                            .id(friend.getId())
                            .username(friend.getUsername())
                            .displayName(friend.getDisplayName())
                            .isLive(friend.getIsLive())
                            .friendshipCreatedAt(friendship.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeFriend(Long friendId) {
        User currentUser = authService.getCurrentUser();
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Remove bidirectional friendship
        friendRepository.deleteByUserAndFriend(currentUser, friend);
        friendRepository.deleteByUserAndFriend(friend, currentUser);
    }

    public FriendRequestsDto getFriendRequests() {
        User currentUser = authService.getCurrentUser();
        
        List<FriendRequest> sent = friendRequestRepository
                .findByRequesterAndStatus(currentUser, FriendRequestStatus.PENDING);
        List<FriendRequest> received = friendRequestRepository
                .findByRecipientAndStatus(currentUser, FriendRequestStatus.PENDING);

        return FriendRequestsDto.builder()
                .sent(sent.stream().map(this::toDto).collect(Collectors.toList()))
                .received(received.stream().map(this::toDto).collect(Collectors.toList()))
                .build();
    }

    public FriendRequestDto toDto(FriendRequest request) {
        User otherUser = request.getRequester();
        if (request.getRequester().getId().equals(authService.getCurrentUser().getId())) {
            otherUser = request.getRecipient();
        }

        return FriendRequestDto.builder()
                .id(request.getId())
                .requesterId(request.getRequester().getId())
                .recipientId(request.getRecipient().getId())
                .otherUserId(otherUser.getId())
                .otherUsername(otherUser.getUsername())
                .status(request.getStatus().name())
                .createdAt(request.getCreatedAt())
                .build();
    }

    public static class FriendDto {
        private Long id;
        private String username;
        private String displayName;
        private Boolean isLive;
        private java.time.OffsetDateTime friendshipCreatedAt;

        public static FriendDtoBuilder builder() {
            return new FriendDtoBuilder();
        }

        public static class FriendDtoBuilder {
            private Long id;
            private String username;
            private String displayName;
            private Boolean isLive;
            private java.time.OffsetDateTime friendshipCreatedAt;

            public FriendDtoBuilder id(Long id) { this.id = id; return this; }
            public FriendDtoBuilder username(String username) { this.username = username; return this; }
            public FriendDtoBuilder displayName(String displayName) { this.displayName = displayName; return this; }
            public FriendDtoBuilder isLive(Boolean isLive) { this.isLive = isLive; return this; }
            public FriendDtoBuilder friendshipCreatedAt(java.time.OffsetDateTime friendshipCreatedAt) { this.friendshipCreatedAt = friendshipCreatedAt; return this; }

            public FriendDto build() {
                FriendDto dto = new FriendDto();
                dto.id = id;
                dto.username = username;
                dto.displayName = displayName;
                dto.isLive = isLive;
                dto.friendshipCreatedAt = friendshipCreatedAt;
                return dto;
            }
        }

        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getDisplayName() { return displayName; }
        public Boolean getIsLive() { return isLive; }
        public java.time.OffsetDateTime getFriendshipCreatedAt() { return friendshipCreatedAt; }
    }

    public static class FriendRequestDto {
        private Long id;
        private Long requesterId;
        private Long recipientId;
        private Long otherUserId;
        private String otherUsername;
        private String status;
        private java.time.OffsetDateTime createdAt;

        public static FriendRequestDtoBuilder builder() {
            return new FriendRequestDtoBuilder();
        }

        public static class FriendRequestDtoBuilder {
            private Long id;
            private Long requesterId;
            private Long recipientId;
            private Long otherUserId;
            private String otherUsername;
            private String status;
            private java.time.OffsetDateTime createdAt;

            public FriendRequestDtoBuilder id(Long id) { this.id = id; return this; }
            public FriendRequestDtoBuilder requesterId(Long requesterId) { this.requesterId = requesterId; return this; }
            public FriendRequestDtoBuilder recipientId(Long recipientId) { this.recipientId = recipientId; return this; }
            public FriendRequestDtoBuilder otherUserId(Long otherUserId) { this.otherUserId = otherUserId; return this; }
            public FriendRequestDtoBuilder otherUsername(String otherUsername) { this.otherUsername = otherUsername; return this; }
            public FriendRequestDtoBuilder status(String status) { this.status = status; return this; }
            public FriendRequestDtoBuilder createdAt(java.time.OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

            public FriendRequestDto build() {
                FriendRequestDto dto = new FriendRequestDto();
                dto.id = id;
                dto.requesterId = requesterId;
                dto.recipientId = recipientId;
                dto.otherUserId = otherUserId;
                dto.otherUsername = otherUsername;
                dto.status = status;
                dto.createdAt = createdAt;
                return dto;
            }
        }

        public Long getId() { return id; }
        public Long getRequesterId() { return requesterId; }
        public Long getRecipientId() { return recipientId; }
        public Long getOtherUserId() { return otherUserId; }
        public String getOtherUsername() { return otherUsername; }
        public String getStatus() { return status; }
        public java.time.OffsetDateTime getCreatedAt() { return createdAt; }
    }

    public static class FriendRequestsDto {
        private List<FriendRequestDto> sent;
        private List<FriendRequestDto> received;

        public static FriendRequestsDtoBuilder builder() {
            return new FriendRequestsDtoBuilder();
        }

        public static class FriendRequestsDtoBuilder {
            private List<FriendRequestDto> sent;
            private List<FriendRequestDto> received;

            public FriendRequestsDtoBuilder sent(List<FriendRequestDto> sent) { this.sent = sent; return this; }
            public FriendRequestsDtoBuilder received(List<FriendRequestDto> received) { this.received = received; return this; }

            public FriendRequestsDto build() {
                FriendRequestsDto dto = new FriendRequestsDto();
                dto.sent = sent;
                dto.received = received;
                return dto;
            }
        }

        public List<FriendRequestDto> getSent() { return sent; }
        public List<FriendRequestDto> getReceived() { return received; }
    }
}

