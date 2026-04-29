package com.school.social.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public final class JwtUtil {
    private static final String SECRET = "change-this-secret";
    private static final String CLAIM_PASSWORD_CHANGED_AT = "pwdAt";
    private static final long EXPIRE_MS = 7L * 24 * 60 * 60 * 1000;

    private JwtUtil() {
    }

    public static String generateToken(Long userId, String username) {
        return generateToken(userId, username, null);
    }

    public static String generateToken(Long userId, String username, LocalDateTime passwordChangedAt) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + EXPIRE_MS);
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(SignatureAlgorithm.HS256, SECRET);
        if (passwordChangedAt != null) {
            builder.claim(CLAIM_PASSWORD_CHANGED_AT, toEpochMillis(passwordChangedAt));
        }
        return builder.compact();
    }

    public static Long getUserId(String token) {
        Claims claims = parse(token);
        if (claims == null) {
            return null;
        }
        try {
            return Long.valueOf(claims.getSubject());
        } catch (Exception ex) {
            return null;
        }
    }

    public static String getUsername(String token) {
        Claims claims = parse(token);
        if (claims == null) {
            return null;
        }
        Object username = claims.get("username");
        return username == null ? null : String.valueOf(username);
    }

    public static Long getPasswordChangedAtMillis(String token) {
        Claims claims = parse(token);
        if (claims == null) {
            return null;
        }
        Object value = claims.get(CLAIM_PASSWORD_CHANGED_AT);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private static Claims parse(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception ex) {
            return null;
        }
    }

    private static long toEpochMillis(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
