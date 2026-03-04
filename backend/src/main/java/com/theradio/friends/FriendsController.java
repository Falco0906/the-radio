package com.theradio.friends;

import com.theradio.auth.AuthService;
import com.theradio.friends.FriendsService.FriendDto;
import com.theradio.friends.FriendsService.FriendRequestDto;
import com.theradio.friends.FriendsService.FriendRequestsDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendsController {
    public FriendsController(FriendsService friendsService) {
        this.friendsService = friendsService;
    }


    private final FriendsService friendsService;

    @GetMapping
    public ResponseEntity<List<FriendDto>> getFriends() {
        return ResponseEntity.ok(friendsService.getFriends());
    }

    @GetMapping("/requests")
    public ResponseEntity<FriendRequestsDto> getFriendRequests() {
        return ResponseEntity.ok(friendsService.getFriendRequests());
    }

    @GetMapping("/search")
    public ResponseEntity<List<FriendsService.UserDto>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(friendsService.searchUsers(query));
    }

    @PostMapping("/requests")
    public ResponseEntity<FriendRequestDto> sendFriendRequest(@RequestBody SendFriendRequestDto request) {
        var friendRequest = friendsService.sendFriendRequest(request.getRecipientId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(friendsService.toDto(friendRequest));
    }

    @PutMapping("/requests/{requestId}")
    public ResponseEntity<Void> respondToFriendRequest(
            @PathVariable Long requestId,
            @RequestBody RespondToRequestDto request) {
        boolean accept = "ACCEPT".equalsIgnoreCase(request.getAction());
        friendsService.respondToFriendRequest(requestId, accept);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/add/{userId}")
    public ResponseEntity<Map<String, String>> addFriend(@PathVariable Long userId) {
        try {
            friendsService.addFriend(userId);
            return ResponseEntity.ok(Map.of("status", "added"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> removeFriend(@PathVariable Long friendId) {
        friendsService.removeFriend(friendId);
        return ResponseEntity.ok().build();
    }

    public static class SendFriendRequestDto {
        private Long recipientId;

        public Long getRecipientId() { return recipientId; }
        public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }
    }

    public static class RespondToRequestDto {
        private String action; // "ACCEPT" or "REJECT"

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }
}

