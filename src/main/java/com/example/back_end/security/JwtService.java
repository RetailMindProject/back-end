package com.example.back_end.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private Long jwtExpiration;

    // ✅ Updated - now accepts userId
    public String generateToken(String email, String role, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);  // ✅ Add userId to claims
        return createToken(claims, email);
    }

* @param email User email (used as subject)
 * @param role User role
 * @param userId Numeric user ID (for RAG service to match with product data)
 * @param firstName User first name
 * @param lastName User last name
 * @return JWT token
 */
public String generateToken(String email, String role, Integer userId, String firstName, String lastName) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("role", role);
    if (userId != null) claims.put("userId", userId);
    if (firstName != null) claims.put("firstName", firstName);
    if (lastName != null) claims.put("lastName", lastName);
    return createToken(claims, email);
}

// ✅ Backward compatibility (old callers)
public String generateToken(String email, String role) {
    return generateToken(email, role, null, null, null);
}

// ✅ If you already had a 3-args version before, keep it too
public String generateToken(String email, String role, Integer userId) {
    return generateToken(email, role, userId, null, null);
}

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // ✅ NEW - Extract userId from token
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userIdObj = claims.get("userId");

        if (userIdObj == null) {
            return null;
        }

        // Handle both Integer and Long
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }

        return null;
    }

    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}