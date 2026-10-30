package com.ems.security;

import com.ems.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Issues and verifies HS256-signed JWTs. The token carries user id, role and employee id,
 * so authorization needs no database lookup per request. Tokens expire after a fixed time.
 */
@Component
public class JwtService {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_EMPLOYEE_ID = "eid";

    private final SecretKey key;
    private final Duration validity;

    public JwtService(@Value("${app.security.jwt-secret}") String base64Secret,
                      @Value("${app.security.jwt-expiration-minutes}") long expirationMinutes) {
        byte[] secret = Base64.getDecoder().decode(base64Secret);
        if (secret.length < 32) {
            throw new IllegalStateException("app.security.jwt-secret must be a Base64-encoded key of at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.validity = Duration.ofMinutes(expirationMinutes);
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }

    public IssuedToken issue(AppUserPrincipal user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(validity);
        String token = Jwts.builder()
                .subject(user.username())
                .claim(CLAIM_USER_ID, user.userId())
                .claim(CLAIM_ROLE, user.role().name())
                .claim(CLAIM_EMPLOYEE_ID, user.employeeId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /**
     * @throws JwtException if the signature is wrong, the token is expired or malformed
     */
    public AppUserPrincipal parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Number userId = claims.get(CLAIM_USER_ID, Number.class);
        Number employeeId = claims.get(CLAIM_EMPLOYEE_ID, Number.class);
        return new AppUserPrincipal(
                userId == null ? null : userId.longValue(),
                claims.getSubject(),
                UserRole.valueOf(claims.get(CLAIM_ROLE, String.class)),
                employeeId == null ? null : employeeId.longValue());
    }
}
