package org.example.clientservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@Order(2)
public class JwtAuthFilter extends OncePerRequestFilter {
    
    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;
    
    //  des paths publics (sans authentification)
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/clients/stats",
        "/api/clients/exists",
        "/api/clients/exists/by-email",
        "/api/clients/public/",
        "/api/documents/files/",
        "/actuator/health",
        "/actuator/info",
        "/swagger-ui",
        "/v3/api-docs"
    );
    
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        if (isPublicPath(path)) {
            log.debug("public, authentification ignorée: {}", path);
            filterChain.doFilter(request, response);
            return;
        }
        
        log.debug("- Requête reçue: {}", path);
        
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("de token JWT pour: {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Authentification requise\"}");
            return;
        }
        
        final String token = authHeader.substring(7);
        log.debug("reçu: {}...", token.substring(0, Math.min(50, token.length())));
        
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            
            log.debug("valide - Email: {}, Rôle: {}", email, role);
            
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                log.debug("créée pour {} avec autorité: {}", email, authority);
                
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        Collections.singletonList(authority)
                );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("définie dans le contexte Spring Security");
            }
        } catch (ExpiredJwtException e) {
            log.warn("expiré: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token expiré\"}");
            return;
        } catch (SignatureException e) {
            log.warn("invalide: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Signature token invalide\"}");
            return;
        } catch (MalformedJwtException e) {
            log.warn("malformé: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token malformé\"}");
            return;
        } catch (Exception e) {
            log.error("validation token: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token invalide\"}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Vérifie si le chemin est public (ne nécessite pas d'authentification)
     */
    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            //  si le chemin commence par le path public
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }
}