package org.example.clientservice.controller;

import org.example.clientservice.dto.request.ClientRequest;
import org.example.clientservice.dto.request.ClientSearchRequest;
import org.example.clientservice.dto.response.ClientResponse;
import org.example.clientservice.dto.response.ClientStatsResponse;
import org.example.clientservice.dto.response.CreditScoreResponse;
import org.example.clientservice.dto.response.PageResponse;
import org.example.clientservice.model.Client;
import org.example.clientservice.model.enums.ClientStatus;
import org.example.clientservice.model.enums.ClientType;
import org.example.clientservice.repository.ClientRepository;
import org.example.clientservice.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {
    
    private final ClientService clientService;
    private final ClientRepository clientRepository;
    
    // 
    //  DE BASE
    // 
    
    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody ClientRequest request) {
        String authenticatedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("d'un client par: {}", authenticatedUser);
        
        ClientResponse response = clientService.createClient(request, authenticatedUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<ClientResponse> getClient(@PathVariable String id) {
        ClientResponse response = clientService.getClient(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/by-email")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<ClientResponse> getClientByEmail(@RequestParam String email) {
        ClientResponse response = clientService.getClientByEmail(email);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint public pour récupérer un client par email sans authentification.
     * GET /api/clients/public/by-email?email=xxx
     */
    @GetMapping("/public/by-email")
    public ResponseEntity<ClientResponse> getPublicClientByEmail(@RequestParam String email) {
        log.info("Recherche client par email: {}", email);
        
        try {
            ClientResponse client = clientService.getClientByEmail(email);
            return ResponseEntity.ok(client);
        } catch (Exception e) {
            log.warn("non trouvé: {}", email);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<List<ClientResponse>> getAllClients() {
        List<ClientResponse> clients = clientService.getAllClients();
        return ResponseEntity.ok(clients);
    }
    
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN' ,'DIRECTEUR_AGENCE')")
    public ResponseEntity<List<ClientResponse>> getClientsByStatus(@PathVariable ClientStatus status) {
        List<ClientResponse> clients = clientService.getClientsByStatus(status);
        return ResponseEntity.ok(clients);
    }

    // 
    //  UNIFIÉE
    // 
    
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN' ,'DIRECTEUR_AGENCE')")
    public ResponseEntity<?> searchClients(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("de clients - q: {}, firstName: {}, lastName: {}, email: {}", q, firstName, lastName, email);
        
        if (q != null && !q.isEmpty()) {
            PageResponse<ClientResponse> response = clientService.searchGlobalPaginated(q, page, size);
            return ResponseEntity.ok(response);
        }
        
        List<Client> clients;
        
        if (firstName != null && lastName != null) {
            clients = clientRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(firstName, lastName);
        } else if (firstName != null) {
            clients = clientRepository.findByFirstNameContainingIgnoreCase(firstName);
        } else if (lastName != null) {
            clients = clientRepository.findByLastNameContainingIgnoreCase(lastName);
        } else if (email != null) {
            Optional<Client> clientOpt = clientRepository.findByEmailContainingIgnoreCase(email);
            clients = clientOpt.map(List::of).orElse(List.of());
        } else {
            clients = clientRepository.findAll();
        }
        
        return ResponseEntity.ok(clients.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }
    
    private ClientResponse mapToResponse(Client client) {
        return ClientResponse.builder()
                .id(client.getId())
                .email(client.getEmail())
                .phoneNumber(client.getPhoneNumber())
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .address(client.getAddress())
                .birthDate(client.getBirthDate())
                .clientType(client.getClientType())
                .status(client.getStatus())
                .creditScore(client.getCreditScore())
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .lastLoginAt(client.getLastLoginAt())
                .build();
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "client-service",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable String id,
            @Valid @RequestBody ClientRequest request) {
        
        ClientResponse response = clientService.updateClient(id, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<Void> deleteClient(@PathVariable String id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<ClientResponse> updateClientStatus(
            @PathVariable String id,
            @RequestParam ClientStatus status) {
        
        ClientResponse response = clientService.updateClientStatus(id, status);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}/credit-score")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<CreditScoreResponse> getCreditScore(@PathVariable String id) {
        CreditScoreResponse response = clientService.getCreditScore(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/credit-score/value")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<Integer> getCreditScoreValue(@PathVariable String id) {
        CreditScoreResponse response = clientService.getCreditScore(id);
        return ResponseEntity.ok(response.getCreditScore());
    }

    // 
    //  DE RECHERCHE SPÉCIFIQUES
    // 
    
    @GetMapping("/by-email-exact")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<ClientResponse> findByEmailExact(@RequestParam String email) {
        ClientResponse response = clientService.findByEmailExact(email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ClientResponse> getMyInfo() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("des informations du client connecté: {}", email);
        ClientResponse response = clientService.findByEmailExact(email);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/by-phone-exact")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<ClientResponse> findByPhoneExact(@RequestParam String phone) {
        ClientResponse response = clientService.findByPhoneExact(phone);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status/{status}/paginated")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<PageResponse<ClientResponse>> getClientsByStatusPaginated(
            @PathVariable ClientStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<ClientResponse> response = clientService.findByStatusPaginated(status, page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/by-type")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<List<ClientResponse>> findByClientType(@RequestParam ClientType clientType) {
        List<ClientResponse> response = clientService.findByClientType(clientType);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/by-min-score")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<List<ClientResponse>> findByMinScore(@RequestParam Integer minScore) {
        List<ClientResponse> response = clientService.findByMinScore(minScore);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/by-max-score")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<List<ClientResponse>> findByMaxScore(@RequestParam Integer maxScore) {
        List<ClientResponse> response = clientService.findByMaxScore(maxScore);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/by-score-between")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<List<ClientResponse>> findByScoreBetween(
            @RequestParam Integer minScore,
            @RequestParam Integer maxScore) {
        List<ClientResponse> response = clientService.findByScoreBetween(minScore, maxScore);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/by-created-after")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<List<ClientResponse>> findByCreatedAfter(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter) {
        List<ClientResponse> response = clientService.findByCreatedAfter(createdAfter);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/search/advanced")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<List<ClientResponse>> searchAdvanced(@RequestBody ClientSearchRequest filters) {
        List<ClientResponse> response = clientService.searchWithFilters(filters);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<PageResponse<ClientResponse>> getAllClientsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<ClientResponse> response = clientService.getAllClientsPaginated(page, size);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/last-login")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<ClientResponse> updateLastLogin(@PathVariable String id) {
        log.info(".updateLastLogin() - Requête reçue pour ID: {}", id);
        ClientResponse response = clientService.updateLastLogin(id);
        log.info("lastLoginAt = {}", response.getLastLoginAt());
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/update-last-login-by-email")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<ClientResponse> updateLastLoginByEmail(@RequestParam String email) {
        ClientResponse response = clientService.updateLastLoginByEmail(email);
        return ResponseEntity.ok(response);
    }

    // 
    //  DE VÉRIFICATION D'EXISTENCE
    // 

    @GetMapping("/{id}/exists")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<Boolean> clientExists(@PathVariable String id) {
        log.info("de l'existence du client par ID: {}", id);
        boolean exists = clientService.clientExists(id);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/exists/by-email")
    public ResponseEntity<Boolean> clientExistsByEmail(@RequestParam String email) {
        log.info("de l'existence du client par email: {}", email);
        boolean exists = clientService.clientExistsByEmail(email);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/exists/by-phone")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<Boolean> clientExistsByPhone(@RequestParam String phone) {
        log.info("de l'existence du client par téléphone: {}", phone);
        boolean exists = clientService.clientExistsByPhone(phone);
        return ResponseEntity.ok(exists);
    }

    // 
    //  CLIENTS 
    // 

    @GetMapping("/stats")
    public ResponseEntity<ClientStatsResponse> getClientStats() {
        log.info("des statistiques clients");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime startOfWeek = now.minusWeeks(1);
        
        long totalClients = clientRepository.count();
        long activeClients = clientRepository.countByStatus(ClientStatus.ACTIVE);
        long inactiveClients = clientRepository.countByStatus(ClientStatus.INACTIVE);
        long pendingClients = clientRepository.countByStatus(ClientStatus.PENDING);
        long suspendedClients = clientRepository.countByStatus(ClientStatus.SUSPENDED);
        long newClientsThisMonth = clientRepository.countByCreatedAtAfter(startOfMonth);
        long newClientsThisWeek = clientRepository.countByCreatedAtAfter(startOfWeek);
        
        Double averageCreditScore = clientRepository.getAverageCreditScore();
        if (averageCreditScore == null) averageCreditScore = 0.0;
        
        Double clientGrowthRate = calculateGrowthRate(totalClients, newClientsThisMonth);
        
        ClientStatsResponse stats = ClientStatsResponse.builder()
            .totalClients(totalClients)
            .activeClients(activeClients)
            .inactiveClients(inactiveClients)
            .pendingClients(pendingClients)
            .suspendedClients(suspendedClients)
            .newClientsThisMonth(newClientsThisMonth)
            .newClientsThisWeek(newClientsThisWeek)
            .clientGrowthRate(clientGrowthRate)
            .averageCreditScore(averageCreditScore)
            .build();
        
        return ResponseEntity.ok(stats);
    }
    
    private Double calculateGrowthRate(long totalClients, long newClientsThisMonth) {
        if (totalClients == 0) return 0.0;
        return (double) newClientsThisMonth / totalClients * 100;
    }

    // 
    //  POUR AGENT
    // 

    /**
     * Endpoint pour qu'un agent récupère SES PROPRES clients
     * Utilise l'email de l'agent authentifié au lieu de l'ID
     */
    @GetMapping("/my-clients")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<List<ClientResponse>> getMyClients() {
        String agentEmail = getCurrentUserEmail();
        log.info("{} récupère ses clients", agentEmail);
        
        List<ClientResponse> clients = clientService.getClientsByAgent(agentEmail);
        return ResponseEntity.ok(clients);
    }
    
    /**
     * Endpoint pour qu'un agent compte SES PROPRES clients
     */
    @GetMapping("/my-clients/count")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<Long> countMyClients() {
        String agentEmail = getCurrentUserEmail();
        log.info("{} compte ses clients", agentEmail);
        long count = clientService.countClientsByAgent(agentEmail);
        return ResponseEntity.ok(count);
    }
    
    /**
     * Statistiques des clients pour l'agent connecté
     */

    @GetMapping("/my-clients/stats")
@PreAuthorize("hasRole('AGENT')")
public ResponseEntity<ClientStatsResponse> getMyClientsStats() {
    String agentEmail = getCurrentUserEmail();
    log.info("des statistiques pour l'agent: {}", agentEmail);
    
    List<ClientResponse> agentClients = clientService.getClientsByAgent(agentEmail);
    
    if (agentClients.isEmpty()) {
        return ResponseEntity.ok(createEmptyClientStats());
    }
    
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0);
    LocalDateTime startOfWeek = now.minusWeeks(1);
    
    long totalClients = agentClients.size();
    
    //   avec l'enum ClientStatus.ACTIVE
    long activeClients = agentClients.stream()
        .filter(c -> c.getStatus() != null && c.getStatus().name().equals("ACTIVE"))
        .count();
    
    long inactiveClients = agentClients.stream()
        .filter(c -> c.getStatus() != null && c.getStatus().name().equals("INACTIVE"))
        .count();
    
    long pendingClients = agentClients.stream()
        .filter(c -> c.getStatus() != null && c.getStatus().name().equals("PENDING"))
        .count();
    
    long suspendedClients = agentClients.stream()
        .filter(c -> c.getStatus() != null && c.getStatus().name().equals("SUSPENDED"))
        .count();
    
    long newClientsThisMonth = agentClients.stream()
        .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(startOfMonth))
        .count();
    long newClientsThisWeek = agentClients.stream()
        .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(startOfWeek))
        .count();
    
    Double averageCreditScore = agentClients.stream()
        .mapToInt(c -> c.getCreditScore() != null ? c.getCreditScore() : 0)
        .average()
        .orElse(0.0);
    
    Double clientGrowthRate = totalClients > 0 ? (double) newClientsThisMonth / totalClients * 100 : 0.0;
    
    ClientStatsResponse stats = ClientStatsResponse.builder()
        .totalClients(totalClients)
        .activeClients(activeClients)
        .inactiveClients(inactiveClients)
        .pendingClients(pendingClients)
        .suspendedClients(suspendedClients)
        .newClientsThisMonth(newClientsThisMonth)
        .newClientsThisWeek(newClientsThisWeek)
        .clientGrowthRate(clientGrowthRate)
        .averageCreditScore(averageCreditScore)
        .build();
    
    return ResponseEntity.ok(stats);
}
    /**
     * Récupérer les IDs des clients de l'agent connecté
     */
    @GetMapping("/my-clients/ids")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<List<String>> getMyClientIds() {
        String agentEmail = getCurrentUserEmail();
        log.info("des IDs clients pour l'agent: {}", agentEmail);
        List<ClientResponse> agentClients = clientService.getClientsByAgent(agentEmail);
        List<String> clientIds = agentClients.stream()
            .map(ClientResponse::getId)
            .collect(Collectors.toList());
        return ResponseEntity.ok(clientIds);
    }

    // 
    //  ADMIN (pour gestion des agents)
    // 

   
    /**
    * Endpoint ADMIN pour voir les clients d'un agent spécifique
    * Accepte à la fois l'UUID et l'email de l'agent
    */
    @GetMapping("/by-agent/{agentIdentifier}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<List<ClientResponse>> getClientsByAgent(@PathVariable String agentIdentifier) {
          log.info("Récupération des clients pour l'agent: {}", agentIdentifier);
    
          List<ClientResponse> clients;
    
          //  l'identifiant contient @, c'est un email
          if (agentIdentifier.contains("@")) {
            clients = clientService.getClientsByAgent(agentIdentifier);
          } else {
                  // , c'est un UUID - il faut d'abord trouver l'email de l'agent
                  String agentEmail = getAgentEmailById(agentIdentifier);
            if (agentEmail != null) {
                  clients = clientService.getClientsByAgent(agentEmail);
           } else {
            clients = clientService.getClientsByAgent(agentIdentifier);
            }
             }
    
       return ResponseEntity.ok(clients);
     }

     /**
      * Récupère l'email d'un agent à partir de son UUID
      * (À implémenter - appel à Auth Service)
      */
     private String getAgentEmailById(String agentId) {
        //  Appeler Auth Service pour récupérer l'email
        //  l'instant, retourner l'ID si c'est déjà un email
       if (agentId.contains("@")) {
          return agentId;
        }
       return null;
     }
    
    /**
     * Statistiques des clients pour un agent spécifique (ADMIN)
     */
    @GetMapping("/by-agent/{agentIdentifier}/stats")
@PreAuthorize("hasAnyRole('ADMIN','DIRECTEUR_AGENCE')")
public ResponseEntity<ClientStatsResponse> getClientStatsForAgent(
        @PathVariable String agentIdentifier,
        @RequestHeader("Authorization") String token) {
    
    log.info("Récupération des statistiques pour l'agent: {}", agentIdentifier);
    
    List<ClientResponse> agentClients = clientService.getClientsByAgent(agentIdentifier, token);
    
    if (agentClients.isEmpty()) {
        return ResponseEntity.ok(createEmptyClientStats());
    }
    
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0);
    LocalDateTime startOfWeek = now.minusWeeks(1);
    
    long totalClients = agentClients.size();
    
    //   avec l'enum ClientStatus.ACTIVE
    long activeClients = agentClients.stream()
        .filter(c -> c.getStatus() == ClientStatus.ACTIVE)
        .count();
    
    long inactiveClients = agentClients.stream()
        .filter(c -> c.getStatus() == ClientStatus.INACTIVE)
        .count();
    
    long pendingClients = agentClients.stream()
        .filter(c -> c.getStatus() == ClientStatus.PENDING)
        .count();
    
    long suspendedClients = agentClients.stream()
        .filter(c -> c.getStatus() == ClientStatus.SUSPENDED)
        .count();
    
    long newClientsThisMonth = agentClients.stream()
        .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(startOfMonth))
        .count();
    long newClientsThisWeek = agentClients.stream()
        .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(startOfWeek))
        .count();
    
    Double averageCreditScore = agentClients.stream()
        .mapToInt(c -> c.getCreditScore() != null ? c.getCreditScore() : 0)
        .average()
        .orElse(0.0);
    
    Double clientGrowthRate = totalClients > 0 ? (double) newClientsThisMonth / totalClients * 100 : 0.0;
    
    ClientStatsResponse stats = ClientStatsResponse.builder()
        .totalClients(totalClients)
        .activeClients(activeClients)
        .inactiveClients(inactiveClients)
        .pendingClients(pendingClients)
        .suspendedClients(suspendedClients)
        .newClientsThisMonth(newClientsThisMonth)
        .newClientsThisWeek(newClientsThisWeek)
        .clientGrowthRate(clientGrowthRate)
        .averageCreditScore(averageCreditScore)
        .build();
    
    return ResponseEntity.ok(stats);
}
    
    /**
     * Récupérer les IDs des clients d'un agent spécifique (ADMIN)
     */
    @GetMapping("/by-agent/{agentId}/client-ids")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<List<String>> getClientIdsByAgent(@PathVariable String agentId) {
        log.info("Récupération des IDs clients pour l'agent: {}", agentId);
        List<ClientResponse> agentClients = clientService.getClientsByAgent(agentId);
        List<String> clientIds = agentClients.stream()
            .map(ClientResponse::getId)
            .collect(Collectors.toList());
        return ResponseEntity.ok(clientIds);
    }

    // 
    //  UTILITAIRES
    // 

    /**
     * Récupère l'email de l'utilisateur authentifié
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }
    
    /**
     * Crée des statistiques client vides
     */
    private ClientStatsResponse createEmptyClientStats() {
        return ClientStatsResponse.builder()
            .totalClients(0L)
            .activeClients(0L)
            .inactiveClients(0L)
            .pendingClients(0L)
            .suspendedClients(0L)
            .newClientsThisMonth(0L)
            .newClientsThisWeek(0L)
            .clientGrowthRate(0.0)
            .averageCreditScore(0.0)
            .build();
    }

    // 
    //  APPELÉS PAR D'AUTRES SERVICES
    // 

    /**
     * Retourne juste le nom complet (prénom + nom) — utilisé par repayment-service
     */
    @GetMapping("/{id}/name")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<String> getClientName(@PathVariable String id) {
        ClientResponse client = clientService.getClient(id);
        return ResponseEntity.ok(client.getFirstName() + " " + client.getLastName());
    }

    /**
     * Retourne les IDs de tous les clients d'une agence — utilisé par repayment-service
     */
    @GetMapping("/by-agency/{agencyId}/ids")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<List<String>> getClientIdsByAgency(@PathVariable String agencyId) {
        log.info("des IDs clients pour l'agence: {}", agencyId);
        List<String> ids = clientRepository.findByAgencyId(agencyId)
                .stream()
                .map(c -> c.getId())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ids);
    }

    @GetMapping("/by-agency/{agencyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<List<ClientResponse>> getClientsByAgency(@PathVariable String agencyId) {
        log.info("des clients pour l'agence: {}", agencyId);
        return ResponseEntity.ok(clientService.getClientsByAgency(agencyId));
    }

    @PatchMapping("/{clientId}/assign-agent")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<ClientResponse> assignClientToAgent(
            @PathVariable String clientId,
            @RequestBody Map<String, String> body) {
        String agentEmail = body.get("agentEmail");
        String agencyId = body.get("agencyId");
        log.info("Assignation client {} à l'agent {} (agence {})", clientId, agentEmail, agencyId);
        return ResponseEntity.ok(clientService.assignClientToAgent(clientId, agentEmail, agencyId));
    }

    /**
     * Endpoint interne — retourne tous les clients créés par une liste d'agents (emails).
     * Appelé par agency-service sans authentification utilisateur.
     */
    @PostMapping("/internal/by-agent-emails")
    public ResponseEntity<List<ClientResponse>> getClientsByAgentEmails(
            @RequestBody List<String> agentEmails) {
        log.info("Recherche clients pour {} agent(s)", agentEmails.size());
        List<ClientResponse> clients = agentEmails.stream()
                .flatMap(email -> clientService.getClientsByAgent(email).stream())
                .distinct()
                .collect(Collectors.toList());
        return ResponseEntity.ok(clients);
    }
}