package org.example.accountservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Utilitaire JWT partagé entre tous les microservices.
 * Ce service NE génère PAS de tokens (c'est le rôle du Service Auth),
 * il les VALIDE uniquement.
 *
 * Le secret JWT doit être identique dans tous les microservices
 * (configurable via variable d'environnement JWT_SECRET).
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /** Construit la clé de signature à partir du secret */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    /**
     * Valide un token JWT.
     * @return true si le token est valide et non expiré
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expiré : {}", ex.getMessage());
        } catch (JwtException ex) {
            log.warn("JWT invalide : {}", ex.getMessage());
        }
        return false;
    }

    /** Extrait le nom d'utilisateur (email) du token */
    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /** Extrait les rôles de l'utilisateur depuis le token */
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

    /** Extrait l'identifiant utilisateur du token */
    public Long getUserIdFromToken(String token) {
        Object userId = getClaims(token).get("userId");
        return userId != null ? Long.parseLong(userId.toString()) : null;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
