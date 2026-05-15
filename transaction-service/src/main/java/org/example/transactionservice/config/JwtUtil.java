package org.example.transactionservice.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expiré : {}", ex.getMessage());
        } catch (JwtException ex) {
            log.warn("JWT invalide : {}", ex.getMessage());
        }
        return false;
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaims(token);
        // Tokens service-à-service : claim "roles" (List)
        Object roles = claims.get("roles");
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        // Tokens utilisateur (auth-service) : claim "role" (String singulier)
        String role = claims.get("role", String.class);
        return role != null ? List.of(role) : List.of();
    }

    public Long getUserIdFromToken(String token) {
        Object userId = getClaims(token).get("userId");
        return userId != null ? Long.parseLong(userId.toString()) : null;
    }

    /** Génère un token interne ADMIN pour les appels service-à-service */
    public String generateServiceToken() {
        return Jwts.builder()
                .subject("transaction-service")
                .claim("roles", List.of("ADMIN"))
                .claim("userId", 0L)
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
