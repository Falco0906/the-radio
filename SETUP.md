# Quick Setup Guide

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 12+
- Node.js 18+
- npm or yarn

## Step-by-Step Setup

### 1. Database Setup

```bash
# Create database
createdb theradio

# Or using psql:
psql -U postgres
CREATE DATABASE theradio;
\q

# Run schema
psql -U postgres -d theradio -f database/schema.sql
```

### 2. Backend Configuration

Edit `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    username: your_db_username
    password: your_db_password
  
  security:
    oauth2:
      client:
        registration:
          spotify:
            client-id: your_spotify_client_id
            client-secret: your_spotify_client_secret

app:
  jwt:
    secret: your-very-long-secret-key-minimum-256-bits
```

### 3. Spotify App Setup

1. Go to https://developer.spotify.com/dashboard
2. Click "Create App"
3. Fill in app details
4. Add redirect URI: `http://localhost:8080/api/platforms/spotify/callback`
5. Copy Client ID and Client Secret to `application.yml`

### 4. Start Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

Backend runs on `http://localhost:8080`

### 5. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5173`

### 6. First Use

1. Open `http://localhost:5173`
2. Register a new account
3. Connect Spotify
4. Toggle "Live" to start sharing
5. Add friends via Profile page

## Troubleshooting

### Database Connection Issues
- Verify PostgreSQL is running: `pg_isready`
- Check credentials in `application.yml`
- Ensure database exists

### Spotify OAuth Issues
- Verify redirect URI matches exactly
- Check Client ID and Secret
- Ensure scopes include: `user-read-currently-playing`, `user-read-playback-state`

### WebSocket Connection Issues
- Check browser console for errors
- Verify JWT token is being sent
- Check CORS settings in `SecurityConfig.java`

### Frontend Build Issues
- Delete `node_modules` and reinstall
- Clear browser cache
- Check Node.js version (18+)

## Development Tips

- Backend logs are in console (set `show-sql: true` in `application.yml` for SQL queries)
- Frontend uses React DevTools for debugging
- WebSocket messages appear in browser console
- Presence polling runs every 5 seconds (configurable)

## Testing the Flow

1. Register two accounts (User A and User B)
2. Both connect Spotify
3. User A sends friend request to User B
4. User B accepts request
5. Both toggle "Live"
6. Start playing music on Spotify
7. See each other's presence updates in real-time
8. Click "Tune In" to open friend's track

