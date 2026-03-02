# The Radio — Feature Roadmap

> After SoundCloud is stable, here is the proposed development roadmap organized into 3 phases.

---

## Phase 1 — MVP (Next 1-3 months)

**Goal:** Core social music experience is solid and shippable.

| Feature | Notes |
|---------|-------|
| 🟢 SoundCloud integration (stable) | **Done in this release** |
| Spotify integration | OAuth + currently-playing polling, same pattern as SoundCloud |
| Friend system (send/accept/reject requests) | Schema + backend already scaffolded |
| Friend feed | See what friends are listening to right now |
| User profile page | Display name, avatar, current track, connected platforms |
| Activity history | Store last N tracks listened per user |
| "Now Playing" WebSocket push | Real-time updates when friends' tracks change |

**Technical work:**
- Spotify `/currently-playing` polling service (poller already exists in `PresencePollingService`)
- Wire WebSocket `PresenceWebSocketService` to frontend `usePresence` hook
- Add avatar upload (S3/Cloudflare R2 or simply a URL field)

---

## Phase 2 — Growth (3-6 months)

**Goal:** Engagement features that bring users back daily.

| Feature | Notes |
|---------|-------|
| YouTube Music integration | Uses YouTube Data API v3 (requires OAuth) |
| Apple Music integration | Uses MusicKit JS on frontend + Apple Music API |
| Activity feed with reactions | Like/comment on what friends are playing |
| Weekly listening stats | Minutes listened, top artists, top tracks |
| Listening streaks | Daily streak counter for activity |
| Notifications | In-app: new friend request, friend started listening |
| Push notifications | Web Push API for mobile Chrome/Safari |
| Track sharing | Share a track as a post to your feed |

**Technical work:**
- Design `activity_feed` table (actor, action_type, target_track, created_at)
- Design `notifications` table with read/unread state
- Evaluate Apple MusicKit JS integration complexity (requires Apple Developer account)

---

## Phase 3 — Scale (6-12 months)

**Goal:** Go from social feature set to a platform.

| Feature | Notes |
|---------|-------|
| Ranking/leaderboard system | Weekly top listeners by genre, artist |
| Artist/genre affinity graph | "You and @friend both love indie rock" |
| Realtime "listening rooms" | Sync music playback with friends |
| iOS / Android app | React Native or native — share codebase logic |
| App Store deployment | Apple App Store + Google Play |
| Public profiles | Discoverable via username URL |
| Spotify/Apple embeds | Embedded player previews for shared tracks |
| Recommendation engine | "Friends who listen to X also like Y" |

**Technical work:**
- Migrate to Redis pub/sub for realtime (beyond WebSocket per-instance limitations)
- Introduce a proper job queue (e.g. BullMQ or Spring Batch) for polling at scale
- Design ranking table with time-windowed aggregations (daily/weekly/all-time)
- Consider read replicas for Postgres at this stage
- Mobile app build with React Native (reuse API and auth patterns)

---

## Platform Integration Priority Order

```
1. Spotify      — Largest user base, best API
2. SoundCloud   — Done ✅
3. YouTube Music — Large user base, API is manageable
4. Apple Music  — Smaller market share, complex API (MusicKit)
```

---

## Architecture Scaling Notes

When traffic grows:

| Component | Current | At Scale |
|-----------|---------|----------|
| Polling | Per-user scheduled job | Batched queue workers (Redis/Kafka) |
| Realtime | WebSocket per JVM instance | Redis pub/sub → scale horizontally |
| DB | Single Postgres | Postgres + read replicas |
| Sessions | Stateless JWT ✅ | Already scales — no sticky sessions needed |
| File storage | None yet | S3/Cloudflare R2 for avatars |
