package org.example.loanservice.client;

import org.example.loanservice.config.FeignClientConfig;
import org.example.loanservice.dto.response.AgencyResponse;
import org.example.loanservice.dto.response.AgentAssignmentResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "AGENCY-SERVICE",
              url = "${agency.service.url:http://localhost:8086}",
              configuration = FeignClientConfig.class)
public interface AgencyServiceClient {
    
    @GetMapping("/api/internal/agencies/by-director-email/{email}")
    AgencyResponse getAgencyByDirectorEmail(@PathVariable("email") String email,
                                             @RequestHeader("Authorization") String token);
    
    @GetMapping("/api/internal/agencies/agent/by-email/{email}")
    AgentAssignmentResponse getAgentAssignmentByEmail(@PathVariable("email") String email,
                                                       @RequestHeader("Authorization") String token);
}