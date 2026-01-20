# The Radio - API Contracts

## REST API Endpoints

### Authentication

#### POST /api/auth/register
Register a new user.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "username": "username",
  "displayName": "Display Name"
}
```

**Response:** 201 Created
```json
{
  "id": 1,
  "email": "user@example.com",
  "username": "username",
  "displayName": "Display Name"
}
```

#### POST /api/auth/login
Login with email and password.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:** 200 OK
```json
{
  "token": "jwt-token-here",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "username": "username",
    "displayName": "Display Name",
    "isLive": false
  }
}
```

#### POST /api/auth/logout
Logout (invalidate session/token).

**Response:** 200 OK

---

### Platform Connections

#### GET /api/platforms/connections
Get all platform connections for current user.

**Response:** 200 OK
```json
[
  {
    "id": 1,
    "platform": "SPOTIFY",
    "platformUserId": "spotify_user_id",
    "connectedAt": "2024-01-01T00:00:00Z"
  }
]
```

#### POST /api/platforms/spotify/connect
Initiate Spotify OAuth flow.

**Response:** 200 OK
```json
{
  "authUrl": "https://accounts.spotify.com/authorize?..."
}
```

#### GET /api/platforms/spotify/callback
OAuth callback endpoint (handled by backend).

**Query params:** `code`, `state`

**Response:** 302 Redirect to frontend with success/error

#### DELETE /api/platforms/connections/{connectionId}
Disconnect a platform.

**Response:** 200 OK

---

### Friends

#### GET /api/friends
Get all friends of current user.

**Response:** 200 OK
```json
[
  {
    "id": 2,
    "username": "friend_username",
    "displayName": "Friend Name",
    "isLive": true,
    "friendshipCreatedAt": "2024-01-01T00:00:00Z"
  }
]
```

#### GET /api/friends/requests
Get pending friend requests (sent and received).

**Response:** 200 OK
```json
{
  "sent": [
    {
      "id": 1,
      "recipientId": 3,
      "recipientUsername": "user3",
      "status": "PENDING",
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "received": [
    {
      "id": 2,
      "requesterId": 4,
      "requesterUsername": "user4",
      "status": "PENDING",
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ]
}
```

#### POST /api/friends/requests
Send a friend request.

**Request:**
```json
{
  "recipientId": 2
}
```

**Response:** 201 Created
```json
{
  "id": 1,
  "requesterId": 1,
  "recipientId": 2,
  "status": "PENDING",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

#### PUT /api/friends/requests/{requestId}
Accept or reject a friend request.

**Request:**
```json
{
  "action": "ACCEPT" // or "REJECT"
}
```

**Response:** 200 OK

#### DELETE /api/friends/{friendId}
Remove a friend.

**Response:** 200 OK

---

### Presence

#### GET /api/presence/current
Get current user's listening state (if live).

**Response:** 200 OK
```json
{
  "userId": 1,
  "platform": "SPOTIFY",
  "trackId": "track_123",
  "trackName": "Song Title",
  "artist": "Artist Name",
  "albumArtUrl": "https://...",
  "progressMs": 45000,
  "durationMs": 180000,
  "isPlaying": true,
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

#### PUT /api/presence/live
Toggle live/invisible status.

**Request:**
```json
{
  "isLive": true
}
```

**Response:** 200 OK
```json
{
  "isLive": true
}
```

---

## WebSocket Events

### Connection
- **Endpoint:** `/ws`
- **Protocol:** STOMP over WebSocket
- **Authentication:** JWT token in connection header or query param

### Subscriptions

#### `/user/queue/presence`
Subscribe to presence updates from friends.

**Event:** `PRESENCE_UPDATE`
```json
{
  "type": "PRESENCE_UPDATE",
  "userId": 2,
  "username": "friend_username",
  "displayName": "Friend Name",
  "platform": "SPOTIFY",
  "trackId": "track_123",
  "trackName": "Song Title",
  "artist": "Artist Name",
  "albumArtUrl": "https://...",
  "progressMs": 45000,
  "durationMs": 180000,
  "isPlaying": true,
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

**Event:** `PRESENCE_OFFLINE`
```json
{
  "type": "PRESENCE_OFFLINE",
  "userId": 2,
  "username": "friend_username"
}
```

**Event:** `PRESENCE_ONLINE`
```json
{
  "type": "PRESENCE_ONLINE",
  "userId": 2,
  "username": "friend_username",
  "displayName": "Friend Name"
}
```

### Client Messages

#### `/app/presence/heartbeat`
Send heartbeat to keep connection alive (optional).

---

## Error Responses

All errors follow this format:

```json
{
  "error": "Error code",
  "message": "Human-readable error message",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

**Common HTTP Status Codes:**
- 400 Bad Request - Invalid input
- 401 Unauthorized - Not authenticated
- 403 Forbidden - Not authorized
- 404 Not Found - Resource not found
- 409 Conflict - Resource conflict (e.g., duplicate friend request)
- 500 Internal Server Error - Server error

