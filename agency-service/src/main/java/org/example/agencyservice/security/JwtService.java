package org.example.agencyservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtService {
    
    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;
    
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public String extractFirstName(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("firstName", String.class);
    }
    
    public String extractLastName(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("lastName", String.class);
    }
    
    public String extractFullName(String token) {
        String firstName = extractFirstName(token);
        String lastName = extractLastName(token);
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return null;
    }
    
    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject();
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}