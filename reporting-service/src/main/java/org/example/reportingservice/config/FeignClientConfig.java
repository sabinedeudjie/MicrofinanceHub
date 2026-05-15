package org.example.reportingservice.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            String token = extractToken();
            if (token != null && !token.isEmpty()) {
                log.info("Propagation token vers: {}", template.url());
                log.info("Token: {}...", token.substring(0, Math.min(30, token.length())));
                template.header("Authorization", "Bearer " + token);
            } else {
                log.warn("️ Aucun token trouvé pour l'appel à: {}", template.url());
                log.warn("   SecurityContext Authentication: {}", 
                    SecurityContextHolder.getContext().getAuthentication());
            }
        };
    }
    
    private String extractToken() {
        //  1: Depuis le SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            log.debug("trouvée: {}", auth.getClass().getSimpleName());
            if (auth.getCredentials() instanceof String) {
                String token = (String) auth.getCredentials();
                log.debug("depuis SecurityContext: {}...", 
                    token != null ? token.substring(0, Math.min(20, token.length())) : "null");
                return token;
            }
        }
        
        //  2: Depuis l'en-tête HTTP
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    log.debug("depuis HTTP Header: {}...", 
                        token.substring(0, Math.min(20, token.length())));
                    return token;
                }
            }
        } catch (Exception e) {
            log.error("extraction token: {}", e.getMessage());
        }
        
        return null;
    }
}