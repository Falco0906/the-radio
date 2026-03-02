# The Radio — Architecture & API Reference

## Architecture Overview

```mermaid
graph TB
    subgraph Browser["Browser (React / Vite)"]
        FE[React App<br/>:5173]
    end

    subgraph Backend["Spring Boot API (:8081)"]
        SEC[SecurityFilterChain<br/>JWT Filter — STATELESS]
        AUTH[/api/auth/*<br/>Register / Login]
        USER[/api/user/me]
        PLAT[/api/platforms/*<br/>Connect / Status]
        SC_CB[/api/platforms/soundcloud/callback<br/>OAuth Callback — permitAll]
    end

    subgraph External["External Services"]
        SC_AUTH[soundcloud.com/connect<br/>OAuth Authorization]
        SC_API[api.soundcloud.com<br/>Token Exchange + /me]
    end

    subgraph DB["PostgreSQL (Docker)"]
        USERS[(users)]
        CONNS[(user_platform_connections)]
    end

    FE -- "POST /api/auth/login" --> AUTH
    FE -- "GET /api/user/me (JWT)" --> USER
    FE -- "GET /api/platforms/soundcloud/connect (JWT)" --> PLAT
    PLAT -- "returns {url}" --> FE
    FE -- "redirect" --> SC_AUTH
    SC_AUTH -- "callback with code + state" --> SC_CB
    SC_CB -- "POST /oauth2/token" --> SC_API
    SC_CB -- "GET /me" --> SC_API
    SC_CB -- "redirect to /connect-platform?connected=soundcloud" --> FE
    AUTH --> USERS
    USER --> USERS
    SC_CB --> CONNS
    PLAT --> CONNS
```

---

## SoundCloud OAuth Flow (Final Clean Version)

```
1. User clicks "Connect SoundCloud" on /connect-platform
   → Frontend: GET /api/platforms/soundcloud/connect
               (JWT in Authorization header)

2. Backend validates JWT, extracts userId
   → Generates a short-lived (5 min) JWT "state" token embedding userId
   → Builds SoundCloud auth URL with state param
   → Returns { url: "https://soundcloud.com/connect?..." }

3. Frontend redirects browser to soundcloud.com/connect

4. User authorizes on SoundCloud

5. SoundCloud redirects to:
     GET /api/platforms/soundcloud/callback?code=XXX&state=JWT_STATE_TOKEN
   → This endpoint is permitAll() — no Spring Security auth needed

6. Backend:
   a. Validates and decodes state JWT → extracts userId (stateless, no session)
   b. POST https://api.soundcloud.com/oauth2/token (exchanges code for access_token)
   c. GET  https://api.soundcloud.com/me
          Authorization: OAuth <access_token>
      → Gets real SoundCloud user ID
   d. Upserts user_platform_connections row
   e. Redirects to: {APP_FRONTEND_URL}/connect-platform?connected=soundcloud

7. Frontend detects ?connected=soundcloud
   → Shows success message
   → Re-fetches /api/platforms/connections to update status
```

**Key Design Decisions:**
- ✅ No HttpSession — fully stateless
- ✅ State = signed JWT (tamper-proof, self-expiring at 5 min)
- ✅ Callback endpoint uses `permitAll()` — SoundCloud redirects here, no JWT
- ✅ User identity carried through OAuth state, not session
- ✅ Platform user ID fetched from real `/me` API (not faked)
- ✅ Upsert prevents duplicate rows on reconnect

---

## API Endpoints Reference

### Auth — `/api/auth/**` (public)

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/auth/register` | Register new user, returns JWT |
| `POST` | `/api/auth/login` | Login, returns JWT + user DTO |
| `POST` | `/api/auth/logout` | Stateless no-op (client clears token) |

### User — `/api/user/**` (requires JWT)

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/api/user/me` | Get current authenticated user profile |

### Platforms — `/api/platforms/**`

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| `GET` | `/api/platforms/soundcloud/connect` | JWT | Get SoundCloud OAuth redirect URL |
| `GET` | `/api/platforms/soundcloud/callback` | None (permitAll) | SoundCloud OAuth callback handler |
| `GET` | `/api/platforms/soundcloud/me` | JWT | Proxy call to SoundCloud /me |
| `GET` | `/api/platforms/connections` | JWT | List all connected platforms |
| `GET` | `/api/platforms/connections/status` | JWT | Map of `{platform: boolean}` connection status |
| `DELETE` | `/api/platforms/connections/{id}` | JWT | Disconnect a platform |

### System

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| `GET` | `/health` | None | Health check for Docker/load balancer |

---

## Environment Variables Reference

### Backend

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET` | ✅ Yes (prod) | weak default | JWT signing secret — min 32 chars |
| `DB_USERNAME` | No | `postgres` | Postgres username |
| `DB_PASSWORD` | ✅ Yes (prod) | `postgres` | Postgres password |
| `SOUNDCLOUD_CLIENT_ID` | ✅ Yes | — | SoundCloud app client ID |
| `SOUNDCLOUD_CLIENT_SECRET` | ✅ Yes | — | SoundCloud app client secret |
| `SOUNDCLOUD_REDIRECT_URI` | ✅ Yes | `http://localhost:8081/...` | Must match SoundCloud app setting |
| `APP_FRONTEND_URL` | ✅ Yes (prod) | `http://localhost:5173` | Frontend base URL for OAuth redirect |
| `APP_ALLOWED_ORIGINS` | ✅ Yes (prod) | `http://localhost:5173,...` | Comma-separated CORS allowed origins |

---

## Deployment Checklist

### Pre-Deploy

- [ ] Generate strong JWT_SECRET: `openssl rand -base64 48`
- [ ] Register SoundCloud OAuth app at [SoundCloud Developers](https://developers.soundcloud.com) with correct redirect URI
- [ ] Set all env vars in your deployment platform (Railway, Fly.io, Render, etc.) — **never commit `.env` with secrets**
- [ ] Verify `SOUNDCLOUD_REDIRECT_URI` matches exactly what's in the SoundCloud developer console

### Docker Compose (local / single-server)

```bash
# 1. Create env file from template
cp backend/.env.example backend/.env
# Fill in secrets

# 2. Clean rebuild
docker-compose down -v
docker-compose up --build

# 3. Verify backend
curl http://localhost:8081/health

# 4. Verify frontend
open http://localhost:5173
```

### Production Checklist

- [ ] Set `APP_FRONTEND_URL` to your real domain (e.g. `https://theradio.app`)
- [ ] Set `APP_ALLOWED_ORIGINS` to your real domain
- [ ] Use a reverse proxy (nginx/Caddy) with HTTPS — never serve JWT over HTTP
- [ ] Set `spring.profiles.active=prod` to use `application-prod.yml` (disables SQL logging)
- [ ] Ensure Postgres is not exposed on `0.0.0.0:5432` in production (remove port mapping)
- [ ] Set up automated DB backups
- [ ] Monitor `/health` endpoint with your uptime service
