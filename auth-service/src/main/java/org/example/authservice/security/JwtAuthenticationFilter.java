package org.example.authservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.example.authservice.service.JwtService;
import org.example.authservice.service.TokenBlacklistService;  //  AJOUTER CET IMPORT

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;
    private final TokenBlacklistService tokenBlacklistService;
    
    private static final List<String> PUBLIC_PATHS = List.of(
        "/auth/register",
        "/auth/login",
        "/auth/refresh-token",
        "/actuator/health",
        "/actuator/info",
        "/auth/forgot-password",
        "/auth/reset-password",
        "/auth/security-questions",
        "/auth/reset-password-with-question",
        "/auth/verify",
        "/auth/resend-verification",
        "/api/public/",   
        "/api/internal/"
    );
    @Override
protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
) throws ServletException, IOException {
    
    String path = request.getServletPath();
    log.info("- Path: {}", path);
    
    //  les endpoints publics
    if (isPublicPath(path)) {
        log.info("   → Path public, bypass");
        filterChain.doFilter(request, response);
        return;
    }
    
    log.info("   → Path protégé, vérification du token");
    
    final String authHeader = request.getHeader("Authorization");
    
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        log.warn("manquant pour: {}", path);
        sendErrorResponse(response, request, HttpStatus.UNAUTHORIZED, "Full authentication is required");
        return;
    }
    
    final String jwt = authHeader.substring(7);
    final String userEmail;
    
    try {
        userEmail = jwtService.extractUsername(jwt);
        log.info("   → Email extrait: {}", userEmail);
    } catch (Exception e) {
        log.warn("invalide pour: {}", path);
        sendErrorResponse(response, request, HttpStatus.UNAUTHORIZED, "Invalid token");
        return;
    }
    
    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        
        //  de blacklist
        if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
            log.warn("blacklisté pour: {}", userEmail);
            sendErrorResponse(response, request, HttpStatus.UNAUTHORIZED, "Token révoqué");
            return;
        }
        
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
        log.info("   → UserDetails chargé: {}", userDetails.getUsername());
        log.info("   → Autorités: {}", userDetails.getAuthorities());
        
        if (jwtService.isTokenValid(jwt, userDetails)) {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()  //  les autorités de la BDD, pas du token
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.info("authentifié: {} avec autorités: {}", userEmail, userDetails.getAuthorities());
        } else {
            log.warn("invalide pour: {}", userEmail);
            sendErrorResponse(response, request, HttpStatus.UNAUTHORIZED, "Invalid token");
            return;
        }
    }
    
    filterChain.doFilter(request, response);
}
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
    
    private void sendErrorResponse(HttpServletResponse response, HttpServletRequest request, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);
        errorResponse.put("path", request.getServletPath());
        
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}