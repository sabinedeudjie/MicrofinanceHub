package org.example.apigateway.filter;

import org.example.apigateway.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {
    
    private final JwtService jwtService;
    
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
        "/api/auth/register",
        "/api/auth/login",
        "/api/auth/refresh-token",
        "/api/auth/forgot-password",
        "/api/auth/reset-password",
        "/api/auth/security-questions",
        "/api/auth/security-question/status",
        "/api/auth/reset-password-with-question",
        "/api/clients/stats",
        "/api/clients/exists",
        "/api/clients/exists/by-email",
        "/actuator/health",
        "/actuator/info",
        "/swagger-ui",
        "/v3/api-docs"
    );
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        
        log.debug("- {} {}", method, path);

        if (isPublicEndpoint(path)) {
            log.debug("public, pas de validation: {}", path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("manquant pour: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        log.debug("reçu: {}...", token.substring(0, Math.min(30, token.length())));

        boolean isValid = jwtService.isTokenValid(token);

        if (!isValid) {
            log.warn("invalide pour: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String username = jwtService.extractUsername(token);
        log.debug("validé pour: {}", username);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
            .header(HttpHeaders.AUTHORIZATION, authHeader)
            .build();
        
        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(mutatedRequest)
            .build();
        
        return chain.filter(mutatedExchange);
    }
    
    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }
    
    @Override
    public int getOrder() {
        return -1;
    }
}