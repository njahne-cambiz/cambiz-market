package com.cambiz.market.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {

    // ✅ HARDCODED - No @Value annotation
    private String secret = "Y2FtYml6LW1hcmtldC1zZWNyZXQta2V5LTIwMjYtbXVzYS02Nzctc2VjdXJpdHktdG9rZW4tY2FtZXJvb24tbWFya2V0";

    private Long expiration = 86400000L;

    private Key signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        signingKey = Keys.hmacShaKeyFor(keyBytes);
        System.out.println("=== JWT PROPERTIES LOADED ===");
        System.out.println("Secret: " + secret);
        System.out.println("Expiration: " + expiration + " ms");
        System.out.println("Secret length: " + secret.length() + " chars");
        System.out.println("First 10 chars: " + secret.substring(0, 10) + "...");
    }

    private Key getSigningKey() {
        return signingKey;
    }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername()) && !isTokenExpired(token);
    }
}