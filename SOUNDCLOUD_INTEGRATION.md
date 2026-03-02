# SoundCloud Integration Guide

## Overview

The Radio now supports SoundCloud as a music platform for sharing listening activity alongside existing Spotify support.

## Setup Instructions

### 1. Create a SoundCloud Developer Application

1. Go to [SoundCloud Developer Portal](https://soundcloud.com/you/apps)
2. Sign in with your SoundCloud account (create one if needed)
3. Click "Register a new application"
4. Fill in the application details:
   - **App Name**: The Radio (or your preferred name)
   - **Application Type**: Select "Application"
   - **Commercial**: Decide based on your use case
5. Accept the terms and create the app
6. You'll receive:
   - **Client ID** (OAuth Client ID)
   - **Client Secret** (OAuth Client Secret)

### 2. Configure OAuth Redirect URI

1. In your SoundCloud app settings, add a Redirect URI:
   - **Development**: `http://localhost:8081/api/platforms/soundcloud/callback`
   - **Production**: `https://yourdomain.com/api/platforms/soundcloud/callback`

### 3. Set Environment Variables

Add the following environment variables to your system or `.env` file:

```bash
export SOUNDCLOUD_CLIENT_ID=your_client_id_here
export SOUNDCLOUD_CLIENT_SECRET=your_client_secret_here
export SOUNDCLOUD_REDIRECT_URI=http://localhost:8081/api/platforms/soundcloud/callback
```

Or for production:
```bash
export SOUNDCLOUD_REDIRECT_URI=https://yourdomain.com/api/platforms/soundcloud/callback
```

### 4. Application Configuration

The application automatically picks up these environment variables. The configuration is defined in `backend/src/main/resources/application.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          soundcloud:
            client-id: ${SOUNDCLOUD_CLIENT_ID:dummy}
            client-secret: ${SOUNDCLOUD_CLIENT_SECRET:dummy}
            scope: non-expiring
            authorization-grant-type: authorization_code
            redirect-uri: ${SOUNDCLOUD_REDIRECT_URI:http://localhost:8081/api/platforms/soundcloud/callback}
        provider:
          soundcloud:
            authorization-uri: https://soundcloud.com/oauth/authorize
            token-uri: https://api.soundcloud.com/oauth2/token
            user-info-uri: https://api.soundcloud.com/me

app:
  soundcloud:
    api-base-url: https://api.soundcloud.com
    polling-interval-seconds: 5
```

## Features Implemented

### Backend Components

#### 1. **SoundCloudApiClient** (`platforms/soundcloud/SoundCloudApiClient.java`)
   - Handles OAuth authorization flow
   - Exchanges authorization codes for access tokens
   - Fetches user profile information
   - Retrieves user activity data

#### 2. **SoundCloudService** (`platforms/soundcloud/SoundCloudService.java`)
   - Manages SoundCloud platform connections
   - Handles OAuth callback processing
   - Stores access tokens securely in the database
   - Validates and retrieves access tokens

#### 3. **PlatformConnectionController Updates**
   - Added `/api/platforms/soundcloud/connect` endpoint to initiate OAuth
   - Added `/api/platforms/soundcloud/callback` endpoint for OAuth callback handling
   - Support for SoundCloud connection management

#### 4. **DTO Classes**
   - `SoundCloudTokenResponse`: Handles OAuth token responses
   - `SoundCloudUserInfo`: Parses user profile data

### Frontend Components

#### **ConnectPlatform.jsx** Updates
   - Added SoundCloud platform option in the platform selection UI
   - Implemented `connectSoundCloud()` function for OAuth initiation
   - Shows connection status (Connected/Not Connected)
   - Same UX pattern as Spotify integration

### Database Changes

#### **PlatformType Enum** (`domain/model/PlatformType.java`)
   - Added `SOUNDCLOUD` as a new platform type
   - Maintains backward compatibility with existing platforms

## OAuth Flow

The SoundCloud integration follows the standard OAuth 2.0 authorization code flow:

1. **User initiates connection** on Connect Platform page
2. **Frontend redirects** to SoundCloud authorization URL
3. **User authorizes** The Radio application on SoundCloud
4. **SoundCloud redirects** to `/api/platforms/soundcloud/callback` with authorization code
5. **Backend exchanges** code for access token
6. **User profile** is fetched and stored
7. **User redirected** back to Connect Platform page with success message
8. **Connection saved** in database for future API calls

## API Endpoints

### OAuth Endpoints
- `POST /api/platforms/soundcloud/connect` - Initiate SoundCloud connection
- `GET /api/platforms/soundcloud/callback` - SoundCloud OAuth callback

### Connection Management
- `GET /api/platforms/connections` - Get all connected platforms
- `DELETE /api/platforms/connections/{connectionId}` - Disconnect a platform

## Token Management

SoundCloud tokens are configured with the `non-expiring` scope, which means:
- Access tokens don't expire automatically
- No refresh token is needed
- Token remains valid indefinitely
- Token is securely stored in the database

## Error Handling

The integration includes comprehensive error handling for:
- Missing OAuth credentials (SOUNDCLOUD_CLIENT_ID, SOUNDCLOUD_CLIENT_SECRET)
- Invalid authorization codes
- Failed token exchanges
- User profile fetch failures
- Network errors

Errors are returned to the user with descriptive messages via URL parameters:
- `?error=error_message` - Shows error notification on the frontend

## Testing

### Local Testing Steps

1. **Start the application**:
   ```bash
   cd backend && mvn spring-boot:run
   cd frontend && npm run dev
   ```

2. **Access the connection page**:
   - Navigate to `http://localhost:5173/connect-platform`

3. **Connect SoundCloud**:
   - Click "Connect" button on SoundCloud platform
   - You'll be redirected to SoundCloud authorization
   - Grant permissions for The Radio app
   - Return to the platform connection page

4. **Verify connection**:
   - Check that SoundCloud shows as "Connected"
   - Connection should appear in the connections list

## Integration with Presence Features

Once connected, SoundCloud can be used for:
- Broadcasting listening activity to friends
- Displaying currently playing track information
- Real-time presence updates
- Presence polling service integration

## Future Enhancements

Potential improvements for SoundCloud integration:
- Fetch real-time "currently playing" information (requires additional API endpoints)
- Support for track metadata (artist, duration, artwork)
- Playlist integration
- Like/favorite tracking
- Advanced scopes for additional permissions

## Troubleshooting

### Issue: "SoundCloud Client ID is not configured"
**Solution**: Ensure `SOUNDCLOUD_CLIENT_ID` environment variable is set before starting the application.

### Issue: "Redirect URI mismatch"
**Solution**: Verify that the redirect URI in SoundCloud app settings exactly matches the `SOUNDCLOUD_REDIRECT_URI` environment variable.

### Issue: "Failed to get user info"
**Solution**: Check that the SoundCloud API is accessible and the access token is valid.

### Issue: Connection not saving
**Solution**: Verify PostgreSQL database is running and accessible at `localhost:5432`.

## References

- [SoundCloud OAuth Documentation](https://soundcloud.com/api)
- [SoundCloud API Reference](https://developers.soundcloud.com/docs)
- [OAuth 2.0 Authorization Code Grant](https://tools.ietf.org/html/rfc6749#section-1.3.1)
