package org.example.agencyservice.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.agencyservice.client.AuthServiceClient;
import org.example.agencyservice.client.AuthServiceClient.TokenInfo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)  // 'exécute après JwtAuthenticationFilter
public class AgentActiveFilter extends OncePerRequestFilter {
    
    private final AuthServiceClient authServiceClient;
    private final ObjectMapper objectMapper;
    
    //  qui ne nécessitent pas de vérification d'agent actif
    private static final List<String> PUBLIC_PATHS = List.of(
        "/actuator/health",
        "/actuator/info",
        "/swagger-ui",
        "/v3/api-docs",
        "/api/internal/",
        "/auth/login",
        "/auth/register",
        "/auth/validate"
    );
    
    //  qui modifient le système (nécessitent un agent actif)
    private static final List<String> MUTATION_PATHS = List.of(
        "/api/accounts",
        "/api/loans/apply",
        "/api/loans/approval",
        "/api/loans/disburse",
        "/api/clients",
        "/api/agency/agents/assign",
        "/api/agency/agents/unassign"
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        //  les chemins publics
        if (isPublicPath(path)) {
            log.debug("public, bypass AgentActiveFilter: {}", path);
            filterChain.doFilter(request, response);
            return;
        }
        
        //  Récupérer le token depuis le contexte Spring Security
        // ( JwtAuthenticationFilter a déjà validé le token et stocké l'authentification)
        String token = extractTokenFromRequest(request);
        
        if (token == null) {
            log.debug("token trouvé pour: {}", path);
            filterChain.doFilter(request, response);
            return;
        }
        
        //  uniquement pour les opérations de modification
        if (isMutationOperation(method, path)) {
            try {
                //  Récupérer les informations du token (passer le token avec Bearer)
                String bearerToken = "Bearer " + token;
                TokenInfo tokenInfo = authServiceClient.getTokenInfo(bearerToken);
                
                if (tokenInfo == null) {
                    log.warn("de récupérer les infos du token pour: {}", path);
                    filterChain.doFilter(request, response);
                    return;
                }
                
                String role = tokenInfo.getRole();
                String email = tokenInfo.getEmail();
                
                log.debug("agent - Rôle: {}, Email: {}, Path: {}", role, email, path);
                
                //  l'utilisateur est un AGENT, vérifier qu'il est actif
                if ("AGENT".equals(role)) {
                    boolean isActive = authServiceClient.isAgentActive(email, bearerToken);
                    
                    if (!isActive) {
                        log.warn(" Agent inactif {} tente de faire une opération interdite: {} {}", 
                            email, method, path);
                        sendErrorResponse(response, HttpStatus.FORBIDDEN, 
                            "Votre compte agent est inactif. Vous ne pouvez pas effectuer cette opération.");
                        return;
                    }
                    log.info(" Agent actif {} autorisé pour: {} {}", email, method, path);
                } else {
                    log.debug("avec rôle {} non soumis à vérification d'activité", role);
                }
                
            } catch (Exception e) {
                log.error("lors de la vérification de l'agent: {}", e.getMessage());
                //  cas d'erreur technique, on laisse passer (le service pourrait être down)
                //  pour la sécurité, on peut aussi bloquer
                log.warn("technique, accès autorisé par défaut pour: {}", path);
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
    
    private boolean isMutationOperation(String method, String path) {
        boolean isMutationMethod = method.equals("POST") || method.equals("PUT") || 
                                   method.equals("PATCH") || method.equals("DELETE");
        boolean isMutationPath = MUTATION_PATHS.stream().anyMatch(path::contains);
        return isMutationMethod && isMutationPath;
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);
        
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}