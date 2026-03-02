package com.theradio.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtTokenProvider {

    private static final String OAUTH_STATE_CLAIM_USER_ID = "uid";
    private static final String OAUTH_STATE_CLAIM_NONCE = "nonce";
    private static final String AUTH_CLAIM_USER_ID = "uid";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateToken(String email, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        var builder = Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate);

        if (userId != null) {
            builder = builder.claim(AUTH_CLAIM_USER_ID, userId);
        }

        return builder
                .signWith(getSigningKey())
                .compact();
    }

    public Long getUserIdFromJWT(String token) {
        if (!validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }

        Claims claims = getAllClaimsFromToken(token);
        Object userIdObj = claims.get(AUTH_CLAIM_USER_ID);
        if (userIdObj instanceof Number n) {
            return n.longValue();
        }
        if (userIdObj instanceof String s) {
            return Long.parseLong(s);
        }

        throw new RuntimeException("Token missing userId");
    }

    public String generateSoundCloudStateToken(Long userId, Duration ttl) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        Duration effectiveTtl = (ttl == null || ttl.isNegative() || ttl.isZero())
            ? Duration.ofMinutes(5)
            : ttl;

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + effectiveTtl.toMillis());

        return Jwts.builder()
            .claim(OAUTH_STATE_CLAIM_USER_ID, userId)
            .claim(OAUTH_STATE_CLAIM_NONCE, UUID.randomUUID().toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    public Long getUserIdFromSoundCloudStateToken(String token) {
        if (!validateToken(token)) {
            throw new RuntimeException("Invalid or expired state");
        }

        Claims claims = getAllClaimsFromToken(token);
        Object userIdObj = claims.get(OAUTH_STATE_CLAIM_USER_ID);
        if (userIdObj instanceof Number n) {
            return n.longValue();
        }
        if (userIdObj instanceof String s) {
            return Long.parseLong(s);
        }

        throw new RuntimeException("Invalid state: missing userId");
    }

    public String generateSpotifyStateToken(Long userId, Duration ttl) {
        return generateStateToken(userId, ttl);
    }

    public Long getUserIdFromSpotifyStateToken(String token) {
        return getUserIdFromStateToken(token);
    }

    private String generateStateToken(Long userId, Duration ttl) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        Duration effectiveTtl = (ttl == null || ttl.isNegative() || ttl.isZero())
                ? Duration.ofMinutes(5)
                : ttl;

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + effectiveTtl.toMillis());

        return Jwts.builder()
                .claim(OAUTH_STATE_CLAIM_USER_ID, userId)
                .claim(OAUTH_STATE_CLAIM_NONCE, UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    private Long getUserIdFromStateToken(String token) {
        if (!validateToken(token)) {
            throw new RuntimeException("Invalid or expired state");
        }

        Claims claims = getAllClaimsFromToken(token);
        Object userIdObj = claims.get(OAUTH_STATE_CLAIM_USER_ID);
        if (userIdObj instanceof Number n) {
            return n.longValue();
        }
        if (userIdObj instanceof String s) {
            return Long.parseLong(s);
        }

        throw new RuntimeException("Invalid state: missing userId");
    }

    public String getEmailFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            getAllClaimsFromToken(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
}
