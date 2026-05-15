package org.example.loanservice.config;

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
                // . Propager le token JWT
                ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String authorization = request.getHeader("Authorization");
                    
                    if (authorization != null && !authorization.isEmpty()) {
                        log.debug(" Token JWT propagé vers: {}", template.url());
                        template.header("Authorization", authorization);
                    }
                }
                
                // .  Ajouter le token interne pour les appels vers Agency Service
                if (template.url().contains("8086") || template.url().contains("/api/internal/")) {
                    log.debug(" Ajout du token interne pour: {}", template.url());
                    template.header("X-Internal-Token", internalToken);
                }
                
            } catch (Exception e) {
                log.warn(" Erreur lors de la propagation du token: {}", e.getMessage());
            }
        };
    }
}


//  org.example.loanservice.config;

//  feign.RequestInterceptor;
//  lombok.extern.slf4j.Slf4j;
//  org.springframework.beans.factory.annotation.Value;
//  org.springframework.context.annotation.Bean;
//  org.springframework.context.annotation.Configuration;
//  org.springframework.web.context.request.RequestContextHolder;
//  org.springframework.web.context.request.ServletRequestAttributes;

//  jakarta.servlet.http.HttpServletRequest;

// 
// 
//  class FeignClientConfig {
    
//     ("${internal.api.token:my-super-secret-internal-token-2026}")
//      String internalToken;
    
//     
//      RequestInterceptor requestInterceptor() {
//          template -> {

//              .info("FEIGN REQUEST ===");
//         .info("{}", template.url());
        
//         //   du token interne avant envoi
//         .info("interne à envoyer: {}", internalToken);
//         .header("X-Internal-Token", internalToken);
//              {
//                 // . Propager le token JWT
//                  attributes = 
//                     () RequestContextHolder.getRequestAttributes();
                
//                  (attributes != null) {
//                      request = attributes.getRequest();
//                      authorization = request.getHeader("Authorization");
                    
//                      (authorization != null && !authorization.isEmpty()) {
//                         .debug("JWT propagé vers: {}", template.url());
//                         .header("Authorization", authorization);
//                     
//                 
                
//                 // . Ajouter le token interne pour tous les appels vers Account Service
//                  (template.url().contains("localhost:8082") || template.url().contains("/api/internal/")) {
//                     .debug("du token interne pour: {}", template.url());
//                     .header("X-Internal-Token", internalToken);
//                 
                
//              catch (Exception e) {
//                 .warn("lors de la propagation du token: {}", e.getMessage());
//             
//         
//     
// 