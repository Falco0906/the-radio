# Environment Variables Setup

## Quick Start

1. Copy the example file:
```bash
cp env.example .env
```

2. Edit `.env` with your actual credentials:
```bash
# Use your preferred editor
nano .env
# or
code .env
```

3. Start the application:
```bash
# Option 1: Use the helper script (recommended)
./start.sh

# Option 2: Export variables manually
export $(cat .env | grep -v '^#' | xargs)
mvn spring-boot:run
```

## What Goes in .env?

- **DB_USERNAME**: Your PostgreSQL username (usually `postgres`)
- **DB_PASSWORD**: Your PostgreSQL password (or leave empty if no password)
- **JWT_SECRET**: A strong secret key for JWT tokens (minimum 256 bits/32 characters)
- **SPOTIFY_CLIENT_ID**: From https://developer.spotify.com/dashboard
- **SPOTIFY_CLIENT_SECRET**: From https://developer.spotify.com/dashboard
- **SPOTIFY_REDIRECT_URI**: Usually `http://localhost:8080/api/platforms/spotify/callback`

## Security Note

⚠️ **Never commit `.env` to Git!** It's already in `.gitignore`, but double-check before pushing.

The `env.example` file is safe to commit - it contains no real credentials.

