package org.example.agencyservice.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import org.example.agencyservice.config.FeignClientConfig;
import org.example.agencyservice.dto.UserDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@FeignClient(name = "AUTH-SERVICE", url = "${auth.service.url:http://localhost:8080}", configuration = FeignClientConfig.class)
public interface AuthServiceClient {
    
    // 
    //  PUBLICS (sans authentification)
    // 
    
    @GetMapping("/api/public/users/{id}")
    UserDTO getUserById(@PathVariable("id") String id);
    
    @GetMapping("/api/public/users/by-email")
    UserDTO getUserByEmail(@RequestParam("email") String email);
    
    @GetMapping("/api/public/agent/exists/{agentId}")
    Map<String, Object> checkAgentExists(@PathVariable("agentId") String agentId);
    
    // 
    //  INTERNES (avec token interne)
    // 
    
    @PutMapping("/internal/users/{userId}/role")
    void updateUserRole(@PathVariable("userId") String userId, 
                        @RequestParam("role") String role);
    
    @PutMapping("/internal/users/{userId}/agency")
    void updateUserAgency(@PathVariable("userId") String userId, 
                          @RequestParam("agencyId") String agencyId,
                          @RequestParam("agencyCode") String agencyCode);
    
    @PutMapping("/internal/users/{userId}/agency/with-assigner")
    void updateUserAgencyWithAssigner(
        @PathVariable("userId") String userId,
        @RequestParam("agencyId") String agencyId,
        @RequestParam("agencyCode") String agencyCode,
        @RequestParam("assignedBy") String assignedBy,
        @RequestParam("assignedByName") String assignedByName
    );
    
    // 
    //  TOKEN
    // 
    
    /**
     * Valide un token JWT et retourne les informations
     */
    @PostMapping("/auth/validate")
    TokenInfo getTokenInfo(@RequestHeader("Authorization") String token);
    
    /**
     * Vérifie si un agent est actif (a une assignation active)
     */
    @GetMapping("/api/public/agent/exists/{email}")
    Map<String, Object> getAgentStatus(@PathVariable("email") String email);

     // si un agent est actif
    @GetMapping("/api/public/agent/active/{email}")
    Boolean isAgentActive(@PathVariable("email") String email, 
                         @RequestHeader("Authorization") String token);
    
    // 
    //  INTERNES
    // 
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class TokenInfo {
        private boolean valid;
        private String username;
        private String role;
        private String firstName;
        private String lastName;
        
        public String getEmail() {
            return username;
        }
    }
}
