# Deployment Guide

## Production Deployment Options

### Option 1: Docker Compose (Recommended)

1. **Set environment variables:**
```bash
export DB_PASSWORD=your_secure_password
export JWT_SECRET=your_very_long_secret_min_256_bits
export SPOTIFY_CLIENT_ID=your_spotify_client_id
export SPOTIFY_CLIENT_SECRET=your_spotify_client_secret
export SPOTIFY_REDIRECT_URI=https://yourdomain.com/api/platforms/spotify/callback
```

2. **Start all services:**
```bash
docker-compose up -d
```

3. **Check logs:**
```bash
docker-compose logs -f
```

### Option 2: Manual Deployment

#### Backend

1. **Build:**
```bash
cd backend
mvn clean package -DskipTests
```

2. **Run:**
```bash
java -jar target/the-radio-1.0.0.jar --spring.profiles.active=prod
```

#### Frontend

1. **Build:**
```bash
cd frontend
npm install
npm run build
```

2. **Serve:**
   - Option A: Use nginx (see `frontend/nginx.conf`)
   - Option B: Serve from Spring Boot static resources
   - Option C: Use any static file server

### Option 3: Spring Boot Serves Frontend

To serve frontend from Spring Boot:

1. Build frontend: `cd frontend && npm run build`
2. Copy `frontend/dist/*` to `backend/src/main/resources/static/`
3. Build backend: `cd backend && mvn clean package`
4. Run: `java -jar target/the-radio-1.0.0.jar`

## Environment Variables

Create a `.env` file or set environment variables:

```bash
# Database
DB_HOST=postgres
DB_USERNAME=postgres
DB_PASSWORD=your_password

# JWT
JWT_SECRET=your_very_long_secret_minimum_256_bits

# Spotify
SPOTIFY_CLIENT_ID=your_client_id
SPOTIFY_CLIENT_SECRET=your_client_secret
SPOTIFY_REDIRECT_URI=https://yourdomain.com/api/platforms/spotify/callback
```

## Production Checklist

- [ ] Set strong JWT_SECRET (minimum 256 bits)
- [ ] Use secure database password
- [ ] Configure Spotify OAuth redirect URI for production domain
- [ ] Enable HTTPS
- [ ] Set up proper CORS origins
- [ ] Configure database backups
- [ ] Set up monitoring/logging
- [ ] Review security settings
- [ ] Test all features end-to-end

## Health Checks

- Backend: `http://yourdomain:8081/health`
- Frontend: `http://yourdomain/`

## Troubleshooting

### Backend won't start
- Check database connection
- Verify environment variables
- Check logs: `docker-compose logs backend`

### Frontend shows blank page
- Check browser console for errors
- Verify API proxy configuration
- Check CORS settings

### Database connection issues
- Verify PostgreSQL is running
- Check credentials
- Ensure database exists

