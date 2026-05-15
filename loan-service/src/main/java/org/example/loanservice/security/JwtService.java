package org.example.loanservice.security;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Component
public class JwtService {
    
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    
    public String extractEmail(String token) {
        try {
            String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
            
            return claims.getSubject();
        } catch (Exception e) {
            log.error("extraction email: {}", e.getMessage());
            return null;
        }
    }
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractRole(String token) {
        try {
            String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
            
            String role = claims.get("role", String.class);
            log.info("extrait du token: {}", role);
            return role;
        } catch (Exception e) {
            log.error("extraction rôle: {}", e.getMessage());
            return null;
        }
    }
}