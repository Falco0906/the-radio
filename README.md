# The Radio

A minimal, retro-styled social music presence platform where users can see what their friends are listening to in near-realtime.

## Features

- **Authentication**: Email/password registration and login
- **Platform Integration**: Spotify OAuth (Apple Music and YouTube Music coming soon)
- **Friends System**: Send, accept, and manage friend requests
- **Live Presence**: Toggle live/invisible status and see friends' listening activity
- **Real-time Updates**: WebSocket-based presence broadcasting
- **Tune-In**: Click to open friend's current track on Spotify

## Tech Stack

### Backend
- Java 17
- Spring Boot 3.2.0
- Maven
- PostgreSQL
- Spring Security + JWT
- WebSockets (STOMP)
- WebFlux for HTTP client

### Frontend
- React 18
- Vite
- React Router
- STOMP.js for WebSockets
- Pure CSS (no frameworks)

## Setup

### Prerequisites
- Java 17+
- Maven
- PostgreSQL
- Node.js 18+

### Database Setup

1. Create PostgreSQL database:
```sql
CREATE DATABASE theradio;
```

2. Run the schema:
```bash
psql -d theradio -f database/schema.sql
```

### Backend Setup

1. Navigate to backend directory:
```bash
cd backend
```

2. Create a `.env` file (copy from `env.example`):
```bash
cp env.example .env
```

3. Edit `.env` file with your credentials:
   - Set database credentials (`DB_USERNAME`, `DB_PASSWORD`)
   - Set Spotify OAuth credentials (get from https://developer.spotify.com/dashboard)
   - Set JWT secret (use a strong secret in production - minimum 256 bits)

4. Build and run:
```bash
# Option 1: Use the helper script (loads .env automatically)
./start.sh

# Option 2: Manual (export env vars first)
export $(cat .env | grep -v '^#' | xargs)
mvn clean install
mvn spring-boot:run
```

**Note:** The `.env` file is gitignored and won't be committed to GitHub. The `env.example` file serves as a template.

Backend will run on `http://localhost:8080`

### Frontend Setup

1. Navigate to frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Run development server:
```bash
npm run dev
```

Frontend will run on `http://localhost:5173`

## Spotify OAuth Setup

1. Go to https://developer.spotify.com/dashboard
2. Create a new app
3. Add redirect URI: `http://localhost:8080/api/platforms/spotify/callback`
4. Copy Client ID and Client Secret to `application.yml`

## Environment Variables

The application uses environment variables for sensitive configuration. Create a `.env` file in the `backend` directory:

```bash
cd backend
cp env.example .env
# Edit .env with your actual values
```

Required variables:
- `DB_USERNAME` - PostgreSQL username (default: postgres)
- `DB_PASSWORD` - PostgreSQL password (default: postgres)
- `SPOTIFY_CLIENT_ID` - From Spotify Developer Dashboard
- `SPOTIFY_CLIENT_SECRET` - From Spotify Developer Dashboard
- `JWT_SECRET` - Secret key for JWT tokens (minimum 256 bits)

The `.env` file is gitignored and won't be committed. Use `env.example` as a template.

## Project Structure

```
radio/
├── backend/
│   ├── src/main/java/com/theradio/
│   │   ├── auth/          # Authentication
│   │   ├── config/         # Configuration
│   │   ├── domain/         # Domain models and repositories
│   │   ├── friends/        # Friends system
│   │   ├── platforms/      # Platform integrations
│   │   │   └── spotify/    # Spotify OAuth and API
│   │   ├── presence/       # Presence and listening state
│   │   ├── security/       # Security and JWT
│   │   └── websocket/      # WebSocket configuration
│   └── src/main/resources/
│       └── application.yml
├── frontend/
│   ├── src/
│   │   ├── components/     # React components
│   │   ├── contexts/       # React contexts
│   │   ├── hooks/          # Custom hooks
│   │   ├── pages/          # Page components
│   │   └── App.jsx
│   └── package.json
├── database/
│   └── schema.sql
└── docs/
    └── API_CONTRACTS.md
```

## API Endpoints

See `docs/API_CONTRACTS.md` for full API documentation.

## Design Philosophy

- **Minimal**: No unnecessary UI elements
- **Retro**: Vintage radio aesthetic with muted green background
- **Information-dense**: Show relevant information clearly
- **Outlined**: Black borders, no fills except background
- **No gradients/shadows/blur**: Pure, simple design
- **Color Scheme**: Muted green (#9CAF88) background with black text and borders

## Development Notes

- Presence polling runs every 5 seconds (configurable)
- WebSocket connections require JWT authentication
- Only friends can see each other's presence
- Users must connect at least one platform to use the app
- Live/invisible toggle controls presence broadcasting

## Deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed deployment instructions.

Quick start with Docker:
```bash
docker-compose up -d
```

## Future Enhancements

- Apple Music integration
- YouTube Music integration
- User search by username
- Mobile app via Capacitor
- Redis caching for presence
- Friend activity history

