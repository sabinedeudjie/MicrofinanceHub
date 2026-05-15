package org.example.agencyservice.controller;
import org.example.agencyservice.dto.request.AgencyRequest;
import org.example.agencyservice.dto.request.AgentAssignmentRequest;
import org.example.agencyservice.dto.request.BulkAgentAssignmentRequest;
import org.example.agencyservice.dto.response.AgencyClientsResponse;
import org.example.agencyservice.dto.response.AgencyResponse;
import org.example.agencyservice.dto.response.AgencyStatsResponse;
import org.example.agencyservice.dto.response.AgentAssignmentResponse;
import org.example.agencyservice.dto.response.BulkAgentAssignmentResponse;
import org.example.agencyservice.exception.AgencyNotFoundException;
import org.example.agencyservice.service.AgencyClientService;
import org.example.agencyservice.service.AgencyService;
import org.example.agencyservice.service.AgencyStatsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/admin/agencies")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AgencyAdminController {
    
    private final AgencyService agencyService;
    private final AgencyStatsService agencyStatsService;
    private final AgencyClientService agencyClientService;
    
    
    
    @PostMapping
    public ResponseEntity<AgencyResponse> createAgency(@Valid @RequestBody AgencyRequest request) {
        String currentUser = getCurrentUserEmail();
        String currentUserName = getCurrentUserName();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(agencyService.createAgency(request, currentUser, currentUserName));
    }
    
    @GetMapping
    public ResponseEntity<List<AgencyResponse>> getAllAgencies() {
        return ResponseEntity.ok(agencyService.getAllAgencies());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AgencyResponse> getAgency(@PathVariable String id) {
        return ResponseEntity.ok(agencyService.getAgency(id));
    }
    
   @PutMapping("/{id}")
public ResponseEntity<AgencyResponse> updateAgency(@PathVariable String id, @Valid @RequestBody AgencyRequest request) {
    log.info("AGENCY - Début");
    log.info("   ID: {}", id);
    log.info("   Code: {}", request.getCode());
    log.info("   Nom: {}", request.getName());
    log.info("   Directeur ID: {}", request.getDirectorId());
    
    String currentUser = getCurrentUserEmail();
    String currentUserName = getCurrentUserName();
    
    try {
        AgencyResponse response = agencyService.updateAgency(id, request, currentUser, currentUserName);
        log.info("AGENCY - Succès - ID: {}", response.getId());
        return ResponseEntity.ok(response);
    } catch (AgencyNotFoundException e) {
        log.warn("non trouvée: {}", id);
        return ResponseEntity.notFound().build();
    } catch (Exception e) {
        log.error("lors de la mise à jour: {}", e.getMessage());
        return ResponseEntity.badRequest().body(null);
    }
}
    
   @DeleteMapping("/{id}")
public ResponseEntity<Void> deleteAgency(@PathVariable String id) {
    log.info(" DELETE AGENCY - ID: {}", id);
    
    try {
        //  delete - changer le status à DELETED au lieu de supprimer
        AgencyResponse agency = agencyService.getAgency(id);
        if (agency == null) {
            log.warn("non trouvée: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        //  le service pour désactiver l'agence
        agencyService.deactivateAgency(id);
        log.info("désactivée (soft delete): {}", id);
        
        return ResponseEntity.noContent().build();
        
    } catch (Exception e) {
        log.error(" Erreur lors de la suppression: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
    
    @PostMapping("/assign-agent")
    public ResponseEntity<AgentAssignmentResponse> assignAgent(@Valid @RequestBody AgentAssignmentRequest request) {
        log.info(" ASSIGN AGENT - Début");
        log.info("   Agent ID: {}", request.getAgentId());
        log.info("   Agency ID: {}", request.getAgencyId());
        log.info("   Reason: {}", request.getReason());
        log.info("   Assignment Date: {}", request.getAssignmentDate());
        log.info("   Reference: {}", request.getReference());
        
        String currentUser = getCurrentUserEmail();
        String currentUserName = getCurrentUserName();
        
        AgentAssignmentResponse response = agencyService.assignAgentToAgency(request, currentUser, currentUserName);
        
        log.info(" Agent assigné avec succès - Reference: {}", response.getReference());
        return ResponseEntity.ok(response);
    }
    
    
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }
    
    private String getCurrentUserName() {
        return getCurrentUserEmail();
    }
    
    @DeleteMapping("/agents/{agentId}/unassign")
    public ResponseEntity<Void> unassignAgent(@PathVariable String agentId) {
        String currentUser = getCurrentUserEmail();
        String currentUserName = getCurrentUserName();
        agencyService.unassignAgent(agentId, currentUser, currentUserName);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/agents/assignments")
    public ResponseEntity<List<AgentAssignmentResponse>> getAllActiveAssignments() {
        return ResponseEntity.ok(agencyService.getAllActiveAssignments());
    }

    @GetMapping("/{agencyId}/agents")
    public ResponseEntity<List<AgentAssignmentResponse>> getAgencyAgents(@PathVariable String agencyId) {
        return ResponseEntity.ok(agencyService.getAgencyAgents(agencyId));
    }
    
    // ("/{agencyId}/stats")
    //  ResponseEntity<?> getAgencyStats(@PathVariable String agencyId) {
    //      ResponseEntity.ok(agencyService.getAgencyStats(agencyId));
    // 
    
    @GetMapping("/{agencyId}/stats")
    public ResponseEntity<AgencyStatsResponse> getAgencyStats(@PathVariable String agencyId) {
         log.info("de l'agence: {}", agencyId);
         return ResponseEntity.ok(agencyStatsService.getAgencyStats(agencyId));
     }

    //  une agence par son code
    @GetMapping("/by-code/{code}")
    public ResponseEntity<AgencyResponse> getAgencyByCode(@PathVariable String code) {
        log.info("de l'agence par code: {}", code);
        return ResponseEntity.ok(agencyService.getAgencyByCode(code));
    }

    ///Désactiver une agence (toggle status)
    @PatchMapping("/{agencyId}/toggle")
    public ResponseEntity<AgencyResponse> toggleAgencyStatus(@PathVariable String agencyId) {
        log.info("status de l'agence: {}", agencyId);
        AgencyResponse response = agencyService.toggleAgencyStatus(agencyId);
        return ResponseEntity.ok(response);
    }
    
    // une agence
    @PatchMapping("/{agencyId}/activate")
    public ResponseEntity<AgencyResponse> activateAgency(@PathVariable String agencyId) {
        log.info("de l'agence: {}", agencyId);
        AgencyResponse response = agencyService.activateAgency(agencyId);
        return ResponseEntity.ok(response);
    }
    
    //   une agence
    @PatchMapping("/{agencyId}/deactivate")
    public ResponseEntity<AgencyResponse> deactivateAgency(@PathVariable String agencyId) {
        log.info("de l'agence: {}", agencyId);
        AgencyResponse response = agencyService.deactivateAgency(agencyId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assign-agents/bulk")
    public ResponseEntity<BulkAgentAssignmentResponse> assignMultipleAgents(
        @Valid @RequestBody BulkAgentAssignmentRequest request) {
    
        log.info("ASSIGN AGENTS - Début");
        log.info("   Agency ID: {}", request.getAgencyId());
        log.info("   Number of agents: {}", request.getAgents().size());
        log.info("   Global reason: {}", request.getGlobalReason());
    
        String currentUser = getCurrentUserEmail();
        String currentUserName = getCurrentUserName();
    
        BulkAgentAssignmentResponse response = agencyService.assignMultipleAgentsToAgency(
        request, currentUser, currentUserName);
    
        log.info("ASSIGN AGENTS - Terminé: {} succès, {} échecs", 
        response.getSuccessCount(), response.getFailedCount());
    
        return ResponseEntity.ok(response);
    }

    // 
    //  POUR DIRECTEUR
    // 
    
    /**
     * Assigner un directeur à une agence
     * PATCH /api/admin/agencies/{agencyId}/director/{directorId}
     */
    @PatchMapping("/{agencyId}/director/{directorId}")
    public ResponseEntity<AgencyResponse> assignDirectorToAgency(
            @PathVariable String agencyId,
            @PathVariable String directorId) {
        
        log.info(" ASSIGN DIRECTOR - Début");
        log.info("   Agency ID: {}", agencyId);
        log.info("   Director ID: {}", directorId);
        
        String currentUser = getCurrentUserEmail();
        String currentUserName = getCurrentUserName();
        
        AgencyResponse response = agencyService.assignDirectorToAgency(agencyId, directorId, currentUser, currentUserName);
        
        log.info(" Directeur assigné avec succès à l'agence: {}", response.getCode());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Désassigner le directeur d'une agence
     * DELETE /api/admin/agencies/{agencyId}/director
     */
    @DeleteMapping("/{agencyId}/director")
    public ResponseEntity<Void> unassignDirectorFromAgency(@PathVariable String agencyId) {
        
        log.info(" UNASSIGN DIRECTOR - Début");
        log.info("   Agency ID: {}", agencyId);
        
        String currentUser = getCurrentUserEmail();
        String currentUserName = getCurrentUserName();
        
        agencyService.unassignDirectorFromAgency(agencyId, currentUser, currentUserName);
        
        log.info(" Directeur désassigné de l'agence: {}", agencyId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Changer le directeur d'une agence
     * PUT /api/admin/agencies/{agencyId}/director/{directorId}
     */
    @PutMapping("/{agencyId}/director/{directorId}")
    public ResponseEntity<AgencyResponse> changeDirector(
            @PathVariable String agencyId,
            @PathVariable String directorId) {
        
        log.info(" CHANGE DIRECTOR - Début");
        log.info("   Agency ID: {}", agencyId);
        log.info("   New Director ID: {}", directorId);
        
        String currentUser = getCurrentUserEmail();
        String currentUserName = getCurrentUserName();
        
        // 'abord désassigner l'ancien, puis assigner le nouveau
        agencyService.unassignDirectorFromAgency(agencyId, currentUser, currentUserName);
        AgencyResponse response = agencyService.assignDirectorToAgency(agencyId, directorId, currentUser, currentUserName);
        
        log.info(" Directeur changé avec succès pour l'agence: {}", response.getCode());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Récupérer le directeur d'une agence
     * GET /api/admin/agencies/{agencyId}/director
     */
    @GetMapping("/{agencyId}/director")
    public ResponseEntity<Map<String, Object>> getDirectorByAgency(@PathVariable String agencyId) {
        
        log.info(" GET DIRECTOR - Agency ID: {}", agencyId);
        
        Map<String, Object> director = agencyService.getDirectorByAgency(agencyId);
        
        if (director == null || !(boolean) director.getOrDefault("exists", false)) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(director);
    }

     // 
    //  POUR LES CLIENTS PAR AGENCE
    // 
    
    /**
     * Récupérer tous les clients d'une agence avec leurs comptes
     * GET /api/admin/agencies/{agencyId}/clients
     */
    @GetMapping("/{agencyId}/clients")
    public ResponseEntity<AgencyClientsResponse> getAgencyClients(
            @PathVariable String agencyId,
            @RequestHeader("Authorization") String token) {
        
        log.info(" Récupération des clients pour l'agence: {}", agencyId);
        AgencyClientsResponse response = agencyClientService.getAgencyClientsWithAccounts(agencyId, token);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Vérifier si un client appartient à une agence
     * GET /api/admin/agencies/{agencyId}/client/validate
     */
    @GetMapping("/{agencyId}/client/validate")
    public ResponseEntity<Map<String, Object>> validateClientBelongsToAgency(
            @PathVariable String agencyId,
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String clientEmail,
            @RequestHeader("Authorization") String token) {
        
        log.info(" Validation client pour l'agence: {}", agencyId);
        
        boolean isValid = agencyClientService.validateClientBelongsToAgency(agencyId, clientId, clientEmail, token);
        
        Map<String, Object> response = new HashMap<>();
        response.put("agencyId", agencyId);
        response.put("clientId", clientId);
        response.put("clientEmail", clientEmail);
        response.put("belongsToAgency", isValid);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Récupérer les statistiques détaillées des clients d'une agence
     * GET /api/admin/agencies/{agencyId}/clients/stats
     */
    @GetMapping("/{agencyId}/clients/stats")
    public ResponseEntity<Map<String, Object>> getAgencyClientsStats(
            @PathVariable String agencyId,
            @RequestHeader("Authorization") String token) {
        
        log.info("Statistiques des clients pour l'agence: {}", agencyId);
        
        Map<String, Object> stats = agencyClientService.getAgencyClientsStats(agencyId, token);
        return ResponseEntity.ok(stats);
    }

}