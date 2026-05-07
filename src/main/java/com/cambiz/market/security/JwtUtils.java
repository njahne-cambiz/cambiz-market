package com.cambiz.market.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    private Key signingKey;

    @PostConstruct
    public void init() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            signingKey = Keys.hmacShaKeyFor(keyBytes);
            logger.info("JWT initialized with expiration: {} ms", expiration);
        } catch (Exception e) {
            logger.error("Failed to initialize JWT signing key: {}", e.getMessage());
            throw new RuntimeException("JWT initialization failed", e);
        }
    }

    private Key getSigningKey() { return signingKey; }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return Jwts.builder().claims(claims).subject(username)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey()).compact();
    }

    public String generateTokenWithRoles(String username, List<String> roles, String userType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("userType", userType);
        return Jwts.builder().claims(claims).subject(username)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey()).compact();
    }

    public String extractUsername(String token) { return extractClaim(token, Claims::getSubject); }

    public Date extractExpiration(String token) { return extractClaim(token, Claims::getExpiration); }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith((SecretKey) signingKey).build()
                .parseSignedClaims(token).getPayload();
    }

    private boolean isTokenExpired(String token) { return extractExpiration(token).before(new Date()); }

    public boolean validateToken(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public Long getUserIdFromToken(String token) {
        String subject = extractUsername(token);
        try { return Long.parseLong(subject); }
        catch (NumberFormatException e) { throw new RuntimeException("Cannot extract user ID: " + subject); }
    }
}