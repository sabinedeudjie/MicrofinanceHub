package org.example.repaymentservice.client;

import org.example.repaymentservice.dto.AgentAssignmentResponse;
import org.example.repaymentservice.config.FeignClientConfig;
import org.example.repaymentservice.dto.AgencyInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "AGENCY-SERVICE",
              url = "${agency.service.url:http://localhost:8086}", 
    configuration = FeignClientConfig.class)
public interface AgencyServiceClient {
    
    /**
     * Récupère l'agence d'un client à partir de son ID
     */
    @GetMapping("/api/internal/agencies/client/{clientId}/agency")
    AgencyInfo getClientAgency(@PathVariable("clientId") String clientId,
                                @RequestHeader("Authorization") String token);
    
    /**
     * Récupère l'assignation d'un agent (pour vérifier son agence)
     */
    @GetMapping("/api/internal/agencies/agent/by-email/{email}")
    AgentAssignmentResponse getAgentAssignmentByEmail(@PathVariable("email") String email,
                                                       @RequestHeader("Authorization") String token);
    
    /**
     * Récupère l'agence d'un directeur
     */
    @GetMapping("/api/internal/agencies/by-director-email/{email}")
    AgencyInfo getAgencyByDirectorEmail(@PathVariable("email") String email,
                                         @RequestHeader("Authorization") String token);
}