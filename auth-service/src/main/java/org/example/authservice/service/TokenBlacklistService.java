package org.example.authservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;
    
    /**
     * Obtient la clé de signature
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Extrait l'ID du token (utilise un hash du token comme identifiant)
     */
    private String extractTokenId(String token) {
        return Integer.toHexString(token.hashCode());
    }
    
    /**
     * Récupère la date d'expiration du token
     */
    private Date extractExpiration(String token) {
        try {
            
            Claims claims = Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("lors de l'extraction de l'expiration", e);
            return new Date();
        }
    }
    
    /**
     * Blacklister un token
     */
    public void blacklistToken(String token) {
        try {
            String tokenId = extractTokenId(token);
            Date expiration = extractExpiration(token);
            long remainingTime = expiration.getTime() - System.currentTimeMillis();
            
            if (remainingTime > 0) {
                redisTemplate.opsForValue().set(
                    "blacklist:" + tokenId,
                    "revoked",
                    remainingTime,
                    TimeUnit.MILLISECONDS
                );
                log.debug("blacklisté: {}", tokenId);
            } else {
                log.debug("déjà expiré, non blacklisté");
            }
        } catch (Exception e) {
            log.error("lors du blacklist du token", e);
        }
    }
    
    /**
     * Vérifie si un token est blacklisté
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String tokenId = extractTokenId(token);
            return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + tokenId));
        } catch (Exception e) {
            log.error("lors de la vérification du blacklist", e);
            return false;
        }
    }
    
    /**
     * Blacklister tous les tokens d'un utilisateur
     */
    public void blacklistAllUserTokens(String userId) {
        redisTemplate.opsForSet().add("user_blacklist:" + userId, "revoked");
        redisTemplate.expire("user_blacklist:" + userId, 7, TimeUnit.DAYS);
        log.info("les tokens de l'utilisateur {} ont été blacklistés", userId);
    }
}