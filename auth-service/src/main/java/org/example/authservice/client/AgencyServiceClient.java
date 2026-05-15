package org.example.authservice.client;

import org.example.authservice.dto.response.AgentAssignmentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "AGENCY-SERVICE", url = "${agency.service.url:http://localhost:8086}")
public interface AgencyServiceClient {

    @GetMapping("/api/internal/agencies/agent/by-email/{email}")
    AgentAssignmentResponse getAgentAssignmentByEmail(@PathVariable("email") String email,
                                                       @RequestHeader("Authorization") String token);

    @DeleteMapping("/api/internal/agencies/agent/{agentId}")
    void removeAgentOnDeletion(@PathVariable("agentId") String agentId);

    @DeleteMapping("/api/internal/agencies/director/{directorId}")
    void removeDirectorOnDeletion(@PathVariable("directorId") String directorId);
}