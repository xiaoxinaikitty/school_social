package com.school.social.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public final class JwtUtil {
    private static final String SECRET = "change-this-secret";
    private static final long EXPIRE_MS = 7L * 24 * 60 * 60 * 1000;

    private JwtUtil() {
    }

    public static String generateToken(Long userId, String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + EXPIRE_MS);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
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
}
