package org.example.agencyservice.controller;
import org.example.agencyservice.client.AuthServiceClient;
import org.example.agencyservice.dto.UserDTO;
import org.example.agencyservice.dto.request.AgentAssignmentRequest;
import org.example.agencyservice.dto.response.AgencyClientsResponse;
import org.example.agencyservice.dto.response.AgencyResponse;
import org.example.agencyservice.dto.response.AgentAssignmentResponse;
import org.example.agencyservice.service.AgencyClientService;
import org.example.agencyservice.service.AgencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/agency")
@RequiredArgsConstructor
public class AgencyController {
    
    private final AgencyService agencyService;
    private final AuthServiceClient authServiceClient;
     private final AgencyClientService agencyClientService;
    
    @GetMapping("/my-agency")
    @PreAuthorize("hasAnyRole('DIRECTEUR_AGENCE', 'ADMIN')")
    public ResponseEntity<AgencyResponse> getMyAgency() {
        String userEmail = getCurrentUserEmail();
        log.info("de l'agence pour l'utilisateur: {}", userEmail);
        
        AgencyResponse agency = agencyService.getAgencyByDirectorEmail(userEmail);
        return ResponseEntity.ok(agency);
    }
    
    @GetMapping("/my-agents")
    @PreAuthorize("hasAnyRole('DIRECTEUR_AGENCE', 'ADMIN')")
    public ResponseEntity<List<AgentAssignmentResponse>> getMyAgents() {
        String userEmail = getCurrentUserEmail();
        log.info("des agents pour le directeur: {}", userEmail);
        
        AgencyResponse agency = agencyService.getAgencyByDirectorEmail(userEmail);
        List<AgentAssignmentResponse> agents = agencyService.getAllAgencyAgents(agency.getId());
        return ResponseEntity.ok(agents);
    }
    
    @GetMapping("/my-stats")
    @PreAuthorize("hasAnyRole('DIRECTEUR_AGENCE', 'ADMIN')")
    public ResponseEntity<?> getMyAgencyStats() {
        String userEmail = getCurrentUserEmail();
        log.info("pour le directeur: {}", userEmail);
        
        AgencyResponse agency = agencyService.getAgencyByDirectorEmail(userEmail);
        return ResponseEntity.ok(agencyService.getAgencyStats(agency.getId()));
    }
    
    @PostMapping("/agents/assign")
    @PreAuthorize("hasAnyRole('DIRECTEUR_AGENCE', 'ADMIN')")
    public ResponseEntity<AgentAssignmentResponse> assignAgentToMyAgency(@Valid @RequestBody AgentAssignmentRequest request) {
        String userEmail = getCurrentUserEmail();
        String userFullName = getCurrentUserFullName();
        log.info("d'un agent par le directeur: {} ({})", userFullName, userEmail);
        
        //  l'agence du directeur automatiquement
        AgencyResponse agency = agencyService.getAgencyByDirectorEmail(userEmail);
        
        //  l'ID de l'agence
        request.setAgencyId(agency.getId());
        
        String currentUserEmail = getCurrentUserEmail();
        String currentUserName = getCurrentUserFullName();  //  le nom complet
        
        AgentAssignmentResponse response = agencyService.assignAgentToAgency(request, currentUserEmail, currentUserName);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/agents/{agentId}/unassign")
    @PreAuthorize("hasAnyRole('DIRECTEUR_AGENCE', 'ADMIN')")
    public ResponseEntity<Void> unassignMyAgent(@PathVariable String agentId) {
        String currentUserEmail = getCurrentUserEmail();
        String currentUserName = getCurrentUserFullName();
        agencyService.unassignAgent(agentId, currentUserEmail, currentUserName);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/validate-agent/{agentId}")
    @PreAuthorize("hasAnyRole('DIRECTEUR_AGENCE', 'ADMIN')")
    public ResponseEntity<Boolean> validateAgentBelongsToMyAgency(@PathVariable String agentId) {
        String userEmail = getCurrentUserEmail();
        AgencyResponse agency = agencyService.getAgencyByDirectorEmail(userEmail);
        return ResponseEntity.ok(agencyService.validateAgentBelongsToAgency(agentId, agency.getId()));
    }
    
    /**
     * Récupère l'email de l'utilisateur courant depuis le contexte Spring Security
     */
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
    
    /**
     * Récupère le nom complet de l'utilisateur depuis les détails de l'authentification
     */
    private String getCurrentUserFullName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) auth.getDetails();
            String fullName = (String) details.get("fullName");
            if (fullName != null && !fullName.trim().isEmpty()) {
                return fullName.trim();
            }
            String firstName = (String) details.get("firstName");
            String lastName = (String) details.get("lastName");
            if (firstName != null || lastName != null) {
                return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
            }
        }
        //  utiliser l'email
        return getCurrentUserEmail();
    }
    
    /**
     * Récupère l'ID de l'utilisateur depuis Auth Service (si nécessaire)
     */
    private String getCurrentUserId() {
        String email = getCurrentUserEmail();
        if (email != null) {
            try {
                UserDTO user = authServiceClient.getUserByEmail(email);
                if (user != null) {
                    return user.getId();
                }
            } catch (Exception e) {
                log.error("lors de la récupération de l'ID utilisateur: {}", e.getMessage());
            }
        }
        return email;
    }

    // 
    //  CLIENTS POUR DIRECTEUR D'AGENCE
    // 
    
    /**
     * Récupérer tous les clients de mon agence avec leurs comptes
     * GET /api/agency/my-clients
     */
    @GetMapping("/my-clients")
    @PreAuthorize("hasAnyRole('DIRECTEUR_AGENCE', 'ADMIN')")
    public ResponseEntity<AgencyClientsResponse> getMyAgencyClients(
            @RequestHeader("Authorization") String token) {
        
        String userEmail = getCurrentUserEmail();
        log.info("Récupération des clients pour l'agence du directeur: {}", userEmail);
        
        AgencyResponse agency = agencyService.getAgencyByDirectorEmail(userEmail);
        AgencyClientsResponse response = agencyClientService.getAgencyClientsWithAccounts(agency.getId(), token);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Vérifier si un client appartient à mon agence
     * GET /api/agency/my-clients/validate
     */
    @GetMapping("/my-clients/validate")
    @PreAuthorize("hasAnyRole('DIRECTEUR_AGENCE', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> validateClientBelongsToMyAgency(
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String clientEmail,
            @RequestHeader("Authorization") String token) {
        
        String userEmail = getCurrentUserEmail();
        log.info("Validation client pour l'agence du directeur: {}", userEmail);
        
        AgencyResponse agency = agencyService.getAgencyByDirectorEmail(userEmail);
        boolean isValid = agencyClientService.validateClientBelongsToAgency(agency.getId(), clientId, clientEmail, token);
        
        Map<String, Object> response = new HashMap<>();
        response.put("agencyId", agency.getId());
        response.put("agencyCode", agency.getCode());
        response.put("agencyName", agency.getName());
        response.put("clientId", clientId);
        response.put("clientEmail", clientEmail);
        response.put("belongsToAgency", isValid);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Statistiques des clients de mon agence
     * GET /api/agency/my-clients/stats
     */
    @GetMapping("/my-clients/stats")
    @PreAuthorize("hasAnyRole('DIRECTEUR_AGENCE', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getMyAgencyClientsStats(
            @RequestHeader("Authorization") String token) {

        String userEmail = getCurrentUserEmail();
        log.info("Statistiques des clients pour l'agence du directeur: {}", userEmail);

        AgencyResponse agency = agencyService.getAgencyByDirectorEmail(userEmail);
        Map<String, Object> stats = agencyClientService.getAgencyClientsStats(agency.getId(), token);
        return ResponseEntity.ok(stats);
    }

    @PatchMapping("/agents/{agentId}/toggle-status")
    @PreAuthorize("hasRole('DIRECTEUR_AGENCE')")
    public ResponseEntity<Map<String, Object>> toggleAgentStatus(@PathVariable String agentId) {
        String userEmail = getCurrentUserEmail();
        AgencyResponse agency = agencyService.getAgencyByDirectorEmail(userEmail);
        agencyService.toggleAgentStatus(agentId, agency.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("agentId", agentId);
        return ResponseEntity.ok(result);
    }
}