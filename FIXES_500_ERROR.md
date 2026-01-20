# Fix for HTTP 500 Login Errors

## Issues Identified and Fixed

### 1. **Inadequate Exception Handling in AuthController**
   - **Problem**: Generic `Exception` caught and returned as HTTP 400, losing error context
   - **Fix**: Added specific handling for `UsernameNotFoundException` to return proper 401 Unauthorized response
   - **Impact**: Now properly distinguishes between client errors (401) and server errors (500)

### 2. **Missing Logging for Debugging**
   - **Problem**: No visibility into why login attempts were failing
   - **Fix**: Added SLF4J logging to `AuthService.login()` method:
     - Logs login attempt with email
     - Logs successful login with username
     - Logs errors with full stack trace
   - **Impact**: Server logs now show exactly what's happening during authentication

### 3. **Missing Test User in Database**
   - **Problem**: Schema created but no default user to test login with
   - **Fix**: Added test user data to `schema.sql`:
     - Email: `test@example.com`
     - Username: `testuser`
     - Password: `password123` (BCrypt hashed)
   - **Impact**: Database automatically initializes with a test user for immediate testing

## Modified Files

1. **[AuthController.java](src/main/java/com/theradio/auth/AuthController.java#L29)**
   - Enhanced error handling to catch `UsernameNotFoundException`
   - Added error message details to response

2. **[AuthService.java](src/main/java/com/theradio/auth/AuthService.java)**
   - Added logger instance
   - Added try-catch wrapper around `login()` method
   - Added logging statements for debugging

3. **[schema.sql](../../database/schema.sql)**
   - Added test user insert statement at end of schema
   - Uses `ON CONFLICT DO NOTHING` to prevent errors on re-initialization

## How to Test

1. **Start the PostgreSQL database** with Docker or locally
2. **Run the backend** - it will auto-initialize the database
3. **Login with test credentials**:
   ```json
   {
     "email": "test@example.com",
     "password": "password123"
   }
   ```

## Additional Notes

- The 500 error was likely caused by database connection issues or missing users in the database
- With logging now in place, you'll see detailed error messages in the backend logs
- The test user is only added if it doesn't exist (uses PostgreSQL `ON CONFLICT`)
- Both email and username can be used for login (handled by `UserDetailsServiceImpl`)
