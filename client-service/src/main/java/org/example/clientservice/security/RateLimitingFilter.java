package org.example.clientservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order; 
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int BLOCK_DURATION_MINUTES = 1;
 
    
    @Override
protected void doFilterInternal(HttpServletRequest request,
                               HttpServletResponse response,
                               FilterChain filterChain) throws ServletException, IOException {
    
    String path = request.getRequestURI();
    
    //  le rate limiting seulement sur les endpoints sensibles
    if (isRateLimitedEndpoint(path)) {
        String clientIp = getClientIp(request);
        String key = "rate_limit:" + path + ":" + clientIp;
        try {
            Long requestCount = redisTemplate.opsForValue().increment(key);
            if (requestCount == 1) {
                redisTemplate.expire(key, BLOCK_DURATION_MINUTES, TimeUnit.MINUTES);
            }
            if (requestCount != null && requestCount > MAX_REQUESTS_PER_MINUTE) {
                log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
                return;
            }
        } catch (Exception e) {
            // Redis indisponible : on laisse passer la requête sans bloquer
            log.warn("Redis indisponible, rate limiting désactivé temporairement: {}", e.getMessage());
        }
    }

    filterChain.doFilter(request, response);
}
    // 
    //  void doFilterInternal(HttpServletRequest request,
    //                                 response,
    //                                 filterChain) throws ServletException, IOException {
        
    //      path = request.getRequestURI();
        
    //     //  le rate limiting seulement sur les endpoints sensibles
    //      (isRateLimitedEndpoint(path)) {
    //          clientIp = getClientIp(request);
    //          key = "rate_limit:" + path + ":" + clientIp;
            
    //          attempts = redisTemplate.opsForValue().get(key);
    //          requestCount = attempts == null ? 0 : Integer.parseInt(attempts);
            
    //          (requestCount >= MAX_REQUESTS_PER_MINUTE) {
    //             .warn("limit exceeded for IP: {} on path: {}", clientIp, path);
    //             .setStatus(429);
    //             .setContentType("application/json");
    //             .getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
    //             
    //         
            
    //         .opsForValue().increment(key);
    //         .expire(key, BLOCK_DURATION_MINUTES, TimeUnit.MINUTES);
    //     
        
    //     .doFilter(request, response);
    // 
    
    private boolean isRateLimitedEndpoint(String path) {
        return path.contains("/api/clients") && 
               !path.contains("/actuator") && 
               !path.contains("/swagger");
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}