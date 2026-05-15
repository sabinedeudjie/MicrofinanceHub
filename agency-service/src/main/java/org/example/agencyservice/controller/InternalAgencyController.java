package org.example.agencyservice.controller;
import org.example.agencyservice.client.AccountServiceClient;
import org.example.agencyservice.client.ClientServiceClient;
import org.example.agencyservice.dto.request.AgencyStatsUpdateRequest;
import org.example.agencyservice.dto.response.AgencyInfo;
import org.example.agencyservice.dto.response.AgencyResponse;
import org.example.agencyservice.dto.response.AgentAssignmentResponse;
import org.example.agencyservice.dto.response.ClientInfo;
import org.example.agencyservice.model.Agency;
import org.example.agencyservice.model.AgentAssignment;
import org.example.agencyservice.repository.AgencyRepository;
import org.example.agencyservice.repository.AgentAssignmentRepository;
import org.example.agencyservice.service.AgencyService;
import org.example.agencyservice.service.AgencyStatsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/internal/agencies")
@RequiredArgsConstructor
public class InternalAgencyController {
    
    private final AgencyService agencyService;
    private final AgencyStatsService agencyStatsService;
    private final AgencyRepository agencyRepository;
    private final AgentAssignmentRepository agentAssignmentRepository;
    private final ClientServiceClient  clientServiceClient;
    private final AccountServiceClient accountServiceClient;
    
    @GetMapping("/{agencyId}")
    public ResponseEntity<AgencyResponse> getAgency(@PathVariable String agencyId) {
        return ResponseEntity.ok(agencyService.getAgency(agencyId));
    }
    
    @GetMapping("/by-code/{code}")
    public ResponseEntity<AgencyResponse> getAgencyByCode(@PathVariable String code) {
        return ResponseEntity.ok(agencyService.getAgencyByCode(code));
    }
    
    @GetMapping("/by-director/{directorId}")
    public ResponseEntity<AgencyResponse> getAgencyByDirector(@PathVariable String directorId) {
        return ResponseEntity.ok(agencyService.getAgencyByDirector(directorId));
    }
    
    @GetMapping("/{agencyId}/agents")
    public ResponseEntity<List<AgentAssignmentResponse>> getAgencyAgents(@PathVariable String agencyId) {
        return ResponseEntity.ok(agencyService.getAgencyAgents(agencyId));
    }
    
    @GetMapping("/validate-agent/{agentId}")
    public ResponseEntity<Boolean> validateAgent(@PathVariable String agentId, @RequestParam String agencyId) {
        return ResponseEntity.ok(agencyService.validateAgentBelongsToAgency(agentId, agencyId));
    }
    
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<AgentAssignmentResponse> getAgentAssignment(@PathVariable String agentId) {
        return ResponseEntity.ok(agencyService.getAgentAssignment(agentId));
    }
    
    // ("/agent/by-email/{email}")
    //  ResponseEntity<AgentAssignmentResponse> getAgentAssignmentByEmail(@PathVariable String email) {
    //     .info("[INTERNAL] Récupération de l'assignation d'agent par email: {}", email);
        
    //      assignment = agencyService.getAgentAssignmentByEmail(email);
    //      (assignment == null) {
    //          ResponseEntity.notFound().build();
    //     
    //      ResponseEntity.ok(assignment);
    // 
    
    /**
     * ENDPOINT : Récupérer l'agence par email du directeur
     */
    @GetMapping("/by-director-email/{email}")
    public ResponseEntity<AgencyResponse> getAgencyByDirectorEmail(@PathVariable String email) {
        log.info("[INTERNAL] Récupération de l'agence par email du directeur: {}", email);
        
        Agency agency = agencyRepository.findByDirectorEmail(email)
                .orElse(null);
        
        if (agency == null) {
            log.warn("agence trouvée pour le directeur: {}", email);
            return ResponseEntity.notFound().build();
        }
        
        
        long agentsCount = agentAssignmentRepository.countByAgencyIdAndActiveTrue(agency.getId());
        
        AgencyResponse response = AgencyResponse.builder()
                .id(agency.getId())
                .code(agency.getCode())
                .name(agency.getName())
                .address(agency.getAddress())
                .city(agency.getCity())
                .phoneNumber(agency.getPhoneNumber())
                .email(agency.getEmail())
                .directorId(agency.getDirectorId())
                .directorEmail(agency.getDirectorEmail())
                .directorName(agency.getDirectorName())
                .region(agency.getRegion())
                .status(agency.getStatus())
                .agentsCount(agentsCount)
                .createdAt(agency.getCreatedAt())
                .updatedAt(agency.getUpdatedAt())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Mettre à jour les statistiques d'une agence (appelé par Account Service)
     */
    @PostMapping("/{agencyId}/stats/update")
    public ResponseEntity<Void> updateAgencyStats(
            @PathVariable String agencyId,
            @RequestBody AgencyStatsUpdateRequest request) {
        
        log.info("[INTERNAL] Mise à jour incrémentale des stats pour l'agence: {}", agencyId);
        
        if (request.getNewClients() != null && request.getNewClients() > 0) {
            agencyStatsService.incrementTotalClients(agencyId, request.getNewClients());
        }
        if (request.getNewAccounts() != null && request.getNewAccounts() > 0) {
            agencyStatsService.incrementTotalAccounts(agencyId, request.getNewAccounts());
        }
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Définir les statistiques d'une agence (mode absolu)
     */
    @PostMapping("/{agencyId}/stats/set")
    public ResponseEntity<Void> setAgencyStats(
            @PathVariable String agencyId,
            @RequestBody AgencyStatsUpdateRequest request) {
        
        log.info("[INTERNAL] Définition des stats pour l'agence: {}", agencyId);
        log.info("   Total clients: {}, Total comptes: {}", request.getTotalClients(), request.getTotalAccounts());
        
        if (request.getTotalClients() != null) {
            agencyStatsService.setTotalClients(agencyId, request.getTotalClients());
        }
        if (request.getTotalAccounts() != null) {
            agencyStatsService.setTotalAccounts(agencyId, request.getTotalAccounts());
        }
        
        return ResponseEntity.ok().build();
    }

     /**
     * Vérifie si un client appartient à un agent
     */
    @GetMapping("/client/validate")
    public ResponseEntity<Boolean> validateClientBelongsToAgency(
           @RequestParam String clientId,
           @RequestParam String agentEmail,
           @RequestHeader("Authorization") String token) {
    
           log.info("[INTERNAL] Vérification si le client {} appartient à l'agent {}", clientId, agentEmail);
    
           try {
                 //  l'assignation de l'agent
                 AgentAssignmentResponse agentAssignment = agencyService.getAgentAssignmentByEmail(agentEmail);
        
                 if (agentAssignment == null || !agentAssignment.isActive()) {
                     log.warn("non trouvé ou inactif: {}", agentEmail);
                     return ResponseEntity.ok(false);
                 }
        
                 // le client (utiliser ClientInfo au lieu de ClientResponse)
                 ClientInfo client = clientServiceClient.getClientInfo(clientId, token);
        
                 if (client == null) {
                      log.warn("non trouvé: {}", clientId);
                       return ResponseEntity.ok(false);
                 }
        
                 //  si le client a été créé par cet agent
                 boolean isValid = client.getCreatedBy() != null && client.getCreatedBy().equals(agentEmail);
        
                 log.info("validation: client {} {} à l'agent {} (créé par: {})", 
                 clientId, isValid ? "appartient" : "n'appartient pas", agentEmail, client.getCreatedBy());
        
                 return ResponseEntity.ok(isValid);
        
               } catch (Exception e) {
                  log.error("lors de la validation: {}", e.getMessage());
                  return ResponseEntity.ok(false);
               }
     }

    /**
     * Vérifie si un agent a le droit sur un client
     */
     @GetMapping("/agent/validate-client")
     public ResponseEntity<Boolean> validateAgentHasClient(
           @RequestParam String agentEmail,
           @RequestParam String clientId,
           @RequestHeader("Authorization") String token) {
    
           log.info("[INTERNAL] Vérification si l'agent {} a le droit sur le client {}", agentEmail, clientId);
    
           try {
                  //  l'assignation de l'agent
                  AgentAssignmentResponse agentAssignment = agencyService.getAgentAssignmentByEmail(agentEmail);
        
                  if (agentAssignment == null || !agentAssignment.isActive()) {
                      log.warn("non trouvé ou inactif: {}", agentEmail);
                      return ResponseEntity.ok(false);
                    }
        
                  // le client (utiliser ClientInfo)
                  ClientInfo client = clientServiceClient.getClientInfo(clientId, token);
        
                  if (client == null) {
                      log.warn("non trouvé: {}", clientId);
                      return ResponseEntity.ok(false);
                   }
        
                   // si le client a été créé par cet agent
                   boolean isValid = client.getCreatedBy() != null && client.getCreatedBy().equals(agentEmail);
        
                   log.info("agent {} {} sur le client {} (créé par: {})", 
                   agentEmail, isValid ? "a le droit" : "n'a pas le droit", clientId, client.getCreatedBy());
        
                   return ResponseEntity.ok(isValid);
        
                } catch (Exception e) {
                   log.error("{}", e.getMessage());
                   return ResponseEntity.ok(false);
             }
     }

     /**
     *  Récupère l'agence d'un client
     * Utilisé par Repayment Service pour vérifier les droits
     */
    @GetMapping("/client/{clientId}/agency")
    public ResponseEntity<AgencyInfo> getClientAgency(@PathVariable String clientId,
                                                       @RequestHeader("Authorization") String token) {
        log.info("[INTERNAL] Récupération de l'agence pour le client: {}", clientId);
        
        try {
            // Récupérer les infos du client via Client Service
            ClientInfo clientInfo = clientServiceClient.getClientInfo(clientId, token);
            
            if (clientInfo == null) {
                log.warn("Client non trouvé: {}", clientId);
                return ResponseEntity.notFound().build();
            }
            
            String agencyId = clientInfo.getAgencyId();
            
            if (agencyId == null || agencyId.isEmpty()) {
                log.warn("Le client {} n'a pas d'agence assignée", clientId);
                return ResponseEntity.notFound().build();
            }
            
            //  les détails de l'agence
            Agency agency = agencyRepository.findById(agencyId)
                .orElse(null);
            
            if (agency == null) {
                log.warn("non trouvée pour ID: {}", agencyId);
                return ResponseEntity.notFound().build();
            }
            
            AgencyInfo response = AgencyInfo.builder()
                .id(agency.getId())
                .code(agency.getCode())
                .name(agency.getName())
                .directorId(agency.getDirectorId())
                .directorEmail(agency.getDirectorEmail())
                .status(agency.getStatus())
                .build();
            
            log.info("du client {}: {} ({})", clientId, agency.getCode(), agency.getName());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("lors de la récupération de l'agence: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Supprime l'assignation d'un agent lors de la suppression de son compte utilisateur.
     * N'appelle pas auth-service pour éviter les appels circulaires.
     */
    @DeleteMapping("/agent/{agentId}")
    public ResponseEntity<Void> removeAgentOnDeletion(@PathVariable String agentId) {
        log.info("[INTERNAL] Nettoyage assignation agent supprimé: {}", agentId);
        try {
            agentAssignmentRepository.findByAgentIdAndActiveTrue(agentId).ifPresent(a -> {
                a.setActive(false);
                agentAssignmentRepository.save(a);
                log.info("[INTERNAL] Assignation désactivée pour agent: {}", agentId);
            });
        } catch (Exception e) {
            log.warn("[INTERNAL] Impossible de nettoyer l'assignation de l'agent {}: {}", agentId, e.getMessage());
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Retire le directeur de son agence lors de la suppression de son compte utilisateur.
     * N'appelle pas auth-service pour éviter les appels circulaires.
     */
    @DeleteMapping("/director/{directorId}")
    public ResponseEntity<Void> removeDirectorOnDeletion(@PathVariable String directorId) {
        log.info("[INTERNAL] Nettoyage directeur supprimé: {}", directorId);
        try {
            agencyRepository.findByDirectorId(directorId).ifPresent(agency -> {
                agency.setDirectorId(null);
                agency.setDirectorEmail(null);
                agency.setDirectorName(null);
                agencyRepository.save(agency);
                log.info("[INTERNAL] Directeur retiré de l'agence: {}", agency.getCode());
            });
        } catch (Exception e) {
            log.warn("[INTERNAL] Impossible de retirer le directeur {}: {}", directorId, e.getMessage());
        }
        return ResponseEntity.noContent().build();
    }

    /**
     *  Récupère les informations d'un agent actif
     */
    @GetMapping("/agent/by-email/{email}")
    public ResponseEntity<AgentAssignmentResponse> getAgentAssignmentByEmail(@PathVariable String email) {
        log.info("[INTERNAL] Récupération de l'assignation d'agent par email: {}", email);
        
        AgentAssignment assignment = agentAssignmentRepository.findByAgentEmailAndActiveTrue(email)
            .orElse(null);
        
        if (assignment == null) {
            log.warn("assignation active trouvée pour l'agent: {}", email);
            return ResponseEntity.notFound().build();
        }
        
        Agency agency = agencyRepository.findById(assignment.getAgencyId()).orElse(null);
        
        AgentAssignmentResponse response = AgentAssignmentResponse.builder()
            .id(assignment.getId())
            .agentId(assignment.getAgentId())
            .agentEmail(assignment.getAgentEmail())
            .agentName(assignment.getAgentName())
            .agencyId(assignment.getAgencyId())
            .agencyCode(assignment.getAgencyCode())
            .agencyName(agency != null ? agency.getName() : null)
            .active(assignment.isActive())
            .build();
        
        return ResponseEntity.ok(response);
    }
}