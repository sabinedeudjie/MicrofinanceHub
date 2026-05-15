package org.example.agencyservice.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Configuration
public class FeignClientConfig {
    
    @Value("${internal.api.token:my-super-secret-internal-token-2026}")
    private String internalToken;
    
    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            try {
                //  le token JWT
                ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String authorization = request.getHeader("Authorization");
                    
                    if (authorization != null && !authorization.isEmpty()) {
                        template.header("Authorization", authorization);
                    }
                }
            } catch (Exception e) {
                log.warn("de propager le token JWT: {}", e.getMessage());
            }
            
            //  le token interne pour les appels vers l'endpoint de stats
            if (template.url().contains("/internal/stats/by-agency/")) {
                log.info("ENVOI TOKEN INTERNE ===");
                log.info("{}", template.url());
                log.info("envoyé: {}", internalToken);
                template.header("X-Internal-Token", internalToken);
            }
        };
    }
}