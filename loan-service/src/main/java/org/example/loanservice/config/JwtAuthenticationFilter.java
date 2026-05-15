package org.example.loanservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final ObjectMapper objectMapper;
    
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    
    //  publics (sans authentification)
    private static final List<String> PUBLIC_PATHS = List.of(
        "/actuator/health",
        "/actuator/info",
        "/api/internal/"
    );
    
    //  accessibles aux agents (avec authentification)
    private static final List<String> AGENT_PATHS = List.of(
        "/api/loans/stats",
        "/api/loans/portfolio/stats",
        "/api/loans/stats/by-clients",
        "/api/loans/portfolio/stats/by-clients",
        "/api/loans/client/",
        "/api/loans/"
    );
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        String path = request.getServletPath();
        
        // . Vérifier si le chemin est public (sans authentification)
        if (isPublicPath(path)) {
            log.debug("public: {}", path);
            filterChain.doFilter(request, response);
            return;
        }
        
        // . Pour tous les autres chemins, un token est requis
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("manquant pour: {}", path);
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Full authentication is required to access this resource");
            return;
        }
        
        final String token = authHeader.substring(7);
        
        try {
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            String firstName = claims.get("firstName", String.class);
            String lastName = claims.get("lastName", String.class);
            
            log.debug("validé pour: {} - Rôle: {}", email, role);
            
            //  les autorités
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(email, null, authorities);
            
            //  les détails supplémentaires
            Map<String, Object> details = new HashMap<>();
            details.put("email", email);
            details.put("role", role);
            details.put("firstName", firstName);
            details.put("lastName", lastName);
            authentication.setDetails(details);
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.info("réussie pour: {} (rôle: {}) sur {}", email, role, path);
            
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("de validation du token: {}", e.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }
    
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
    
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);
        
        
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}