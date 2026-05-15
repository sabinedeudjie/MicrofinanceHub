package org.example.agencyservice.service;

import org.example.agencyservice.client.AccountServiceClient;
import org.example.agencyservice.client.AuthServiceClient;
import org.example.agencyservice.dto.UserDTO;
import org.example.agencyservice.dto.request.AgencyRequest;
import org.example.agencyservice.dto.request.AgentAssignmentRequest;
import org.example.agencyservice.dto.request.BulkAgentAssignmentRequest;
import org.example.agencyservice.dto.response.AgencyResponse;
import org.example.agencyservice.dto.response.AgentAssignmentResponse;
import org.example.agencyservice.dto.response.BulkAgentAssignmentResponse;
import org.example.agencyservice.dto.response.AgencyStatsResponse;
import org.example.agencyservice.exception.AgencyNotFoundException;
import org.example.agencyservice.exception.AgentAlreadyAssignedException;
import org.example.agencyservice.model.Agency;
import org.example.agencyservice.model.AgentAssignment;
import org.example.agencyservice.repository.AgencyRepository;
import org.example.agencyservice.repository.AgentAssignmentRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgencyService {
    
    private final AgencyRepository agencyRepository;
    private final AgentAssignmentRepository agentAssignmentRepository;
    private final AuthServiceClient authServiceClient;
    private final AccountServiceClient accountServiceClient;
    
    @Transactional
    public AgencyResponse createAgency(AgencyRequest request, String assignedBy, String assignedByName) {
        log.info("de l'agence: {}", request.getCode());
        
        if (agencyRepository.existsByCode(request.getCode())) {
            throw new RuntimeException(" L'agence avec le code '" + request.getCode() + "' existe déjà");
        }
        
        UserDTO director = validateAndGetDirector(request);
        
        Optional<Agency> existingAgency = agencyRepository.findByDirectorId(director.getId());
        if (existingAgency.isPresent()) {
            throw new RuntimeException(
                String.format(" Le directeur '%s %s' (email: %s) est déjà associé à l'agence '%s' (%s)", 
                    director.getFirstName(), director.getLastName(), director.getEmail(),
                    existingAgency.get().getCode(), existingAgency.get().getName())
            );
        }
        
        Agency agency = Agency.builder()
            .code(request.getCode())
            .name(request.getName())
            .address(request.getAddress())
            .city(request.getCity())
            .phoneNumber(request.getPhoneNumber())
            .email(request.getEmail())
            .region(request.getRegion())
            .directorId(director.getId())
            .directorEmail(director.getEmail())
            .directorName(director.getFirstName() + " " + director.getLastName())
            .status("ACTIVE")
            .build();
        
        agency = agencyRepository.save(agency);
        
        try {
            authServiceClient.updateUserAgency(director.getId(), agency.getId(), agency.getCode());
        } catch (Exception e) {
            log.warn("de mettre à jour l'agence du directeur: {}", e.getMessage());
        }
        
        log.info("créée avec succès. Directeur: {} {}", director.getFirstName(), director.getLastName());
        
        return mapToResponse(agency);
    }
    
    private UserDTO validateAndGetDirector(AgencyRequest request) {
        UserDTO director = null;
        
        if (request.getDirectorId() != null && !request.getDirectorId().isEmpty()) {
            try {
                director = authServiceClient.getUserById(request.getDirectorId());
                if (director == null && request.getDirectorEmail() != null) {
                    director = authServiceClient.getUserByEmail(request.getDirectorEmail());
                }
            } catch (Exception e) {
                log.error("lors de la vérification du directeur: {}", e.getMessage());
                throw new RuntimeException("Impossible de vérifier l'existence du directeur. Service Auth indisponible.");
            }
            
            if (director == null) {
                throw new RuntimeException(
                    String.format("Le directeur avec ID '%s' ou email '%s' n'existe pas dans le système.", 
                        request.getDirectorId(), request.getDirectorEmail())
                );
            }
            
            log.info("trouvé: {} {} ({})", director.getFirstName(), director.getLastName(), director.getEmail());
            
            if (director.getRoles() != null && !director.getRoles().contains("DIRECTEUR_AGENCE")) {
                log.warn("'utilisateur {} n'a pas le rôle DIRECTEUR_AGENCE, il sera mis à jour", director.getEmail());
                try {
                    authServiceClient.updateUserRole(request.getDirectorId(), "DIRECTEUR_AGENCE");
                } catch (Exception e) {
                    log.warn("de mettre à jour le rôle: {}", e.getMessage());
                }
            }
        } else {
            throw new RuntimeException("L'ID du directeur est requis pour créer une agence");
        }
        
        return director;
    }
    
    @Transactional
    public AgencyResponse updateAgency(String id, AgencyRequest request, String updatedBy, String updatedByName) {
        log.info("AGENCY - Service - ID: {}", id);
        
        Agency agency = agencyRepository.findById(id)
            .orElseThrow(() -> new AgencyNotFoundException("Agence non trouvée: " + id));
        
        if (!agency.getCode().equals(request.getCode()) && agencyRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Un autre agence existe déjà avec le code: " + request.getCode());
        }
        
        UserDTO director = null;
        boolean directorChanged = false;
        
        if (request.getDirectorId() != null && !request.getDirectorId().equals(agency.getDirectorId())) {
            directorChanged = true;
            log.info("   Changement de directeur détecté");
            
            try {
                director = authServiceClient.getUserById(request.getDirectorId());
                if (director == null && request.getDirectorEmail() != null) {
                    director = authServiceClient.getUserByEmail(request.getDirectorEmail());
                }
            } catch (Exception e) {
                log.error("lors de la vérification du directeur: {}", e.getMessage());
                throw new RuntimeException("Impossible de vérifier l'existence du nouveau directeur.");
            }
            
            if (director == null) {
                throw new RuntimeException(
                    String.format("Le nouveau directeur avec ID '%s' ou email '%s' n'existe pas.", 
                        request.getDirectorId(), request.getDirectorEmail())
                );
            }
            
            Optional<Agency> existingAgency = agencyRepository.findByDirectorId(request.getDirectorId());
            if (existingAgency.isPresent() && !existingAgency.get().getId().equals(id)) {
                throw new RuntimeException(
                    String.format("Le directeur %s %s est déjà associé à l'agence %s (%s)", 
                        director.getFirstName(), director.getLastName(),
                        existingAgency.get().getCode(), existingAgency.get().getName())
                );
            }
        }
        
        agency.setCode(request.getCode());
        agency.setName(request.getName());
        agency.setAddress(request.getAddress());
        agency.setCity(request.getCity());
        agency.setPhoneNumber(request.getPhoneNumber());
        agency.setEmail(request.getEmail());
        agency.setRegion(request.getRegion());
        agency.setUpdatedAt(LocalDateTime.now());
        
        if (directorChanged && director != null) {
            agency.setDirectorId(director.getId());
            agency.setDirectorEmail(director.getEmail());
            agency.setDirectorName(director.getFirstName() + " " + director.getLastName());
            
            if (agency.getDirectorId() != null && !agency.getDirectorId().equals(request.getDirectorId())) {
                try {
                    authServiceClient.updateUserAgency(agency.getDirectorId(), null, null);
                } catch (Exception e) {
                    log.warn("de mettre à jour l'ancien directeur: {}", e.getMessage());
                }
            }
            
            try {
                authServiceClient.updateUserRole(director.getId(), "DIRECTEUR_AGENCE");
                authServiceClient.updateUserAgency(director.getId(), agency.getId(), agency.getCode());
            } catch (Exception e) {
                log.warn("de mettre à jour le nouveau directeur: {}", e.getMessage());
            }
        }
        
        agency = agencyRepository.save(agency);
        
        log.info("mise à jour avec succès: {} - {}", agency.getCode(), agency.getName());
        
        return mapToResponse(agency);
    }
    @Transactional
public AgentAssignmentResponse assignAgentToAgency(AgentAssignmentRequest request, String assignedBy, String assignedByName) {
    log.info("AGENT - Service");
    
    UserDTO agent = validateAndGetAgent(request);
    
    //  1. D'abord, désactiver l'ancienne assignation (si elle existe)
    Optional<AgentAssignment> existingAssignment = agentAssignmentRepository.findByAgentIdAndActiveTrue(request.getAgentId());
    if (existingAssignment.isPresent()) {
        AgentAssignment old = existingAssignment.get();
        old.setActive(false);
        agentAssignmentRepository.save(old);
        agentAssignmentRepository.flush(); //  la mise à jour immédiate
        log.info("assignation désactivée pour l'agent: {}", request.getAgentId());
    }
    
    // . Vérifier à nouveau qu'il n'y a pas d'assignation active
    if (agentAssignmentRepository.existsByAgentIdAndActiveTrue(request.getAgentId())) {
        throw new AgentAlreadyAssignedException("Agent déjà assigné à une agence");
    }
    
    Agency agency = agencyRepository.findById(request.getAgencyId())
            .orElseThrow(() -> new AgencyNotFoundException("Agence non trouvée: " + request.getAgencyId()));
    
    String reference = generateReference(request.getReference());
    LocalDateTime assignmentDate = parseAssignmentDate(request.getAssignmentDate());
    
    // . Créer la nouvelle assignation
    AgentAssignment assignment = AgentAssignment.builder()
            .agentId(agent.getId())
            .agentEmail(agent.getEmail())
            .agentName(agent.getFirstName() + " " + agent.getLastName())
            .agencyId(agency.getId())
            .agencyCode(agency.getCode())
            .role("AGENT")
            .assignedBy(assignedBy)
            .assignedByName(assignedByName)
            .reason(request.getReason())
            .reference(reference)
            .assignedAt(assignmentDate)
            .active(true)
            .build();
    
    assignment = agentAssignmentRepository.save(assignment);
    
    //  à jour Auth Service
    try {
        authServiceClient.updateUserAgencyWithAssigner(
            agent.getId(), agency.getId(), agency.getCode(), assignedBy, assignedByName);
        authServiceClient.updateUserRole(agent.getId(), "AGENT");
    } catch (Exception e) {
        log.error("mise à jour Auth Service: {}", e.getMessage());
    }
    
    return mapToAssignmentResponse(assignment, agency);
}
    // 
    //  AgentAssignmentResponse assignAgentToAgency(AgentAssignmentRequest request, String assignedBy, String assignedByName) {
    //     .info("AGENT - Service");
    //     .info("   Agent ID: {}", request.getAgentId());
    //     .info("   Agency ID: {}", request.getAgencyId());
        
    //      agent = validateAndGetAgent(request);
        
    //      (agentAssignmentRepository.existsByAgentIdAndActiveTrue(request.getAgentId())) {
    //          new AgentAlreadyAssignedException("Agent déjà assigné à une agence");
    //     
        
    //      agency = agencyRepository.findById(request.getAgencyId())
    //         .(() -> new AgencyNotFoundException("Agence non trouvée: " + request.getAgencyId()));
        
    //     .deactivateAgentAssignments(request.getAgentId());
        
    //      reference = generateReference(request.getReference());
    //      assignmentDate = parseAssignmentDate(request.getAssignmentDate());
        
    //      assignment = AgentAssignment.builder()
    //         .(agent.getId())
    //         .(agent.getEmail())
    //         .(agent.getFirstName() + " " + agent.getLastName())
    //         .(agency.getId())
    //         .(agency.getCode())
    //         .("AGENT")
    //         .(assignedBy)
    //         .(assignedByName)
    //         .(request.getReason())
    //         .(reference)
    //         .(assignmentDate)
    //         .(true)
    //         .();
        
    //      = agentAssignmentRepository.save(assignment);
        
    //     //  À JOUR L'AGENT DANS AUTH SERVICE
    //      {
    //         .updateUserAgencyWithAssigner(
    //             .getId(), agency.getId(), agency.getCode(), assignedBy, assignedByName);
    //         .info("   Agent mis à jour dans Auth Service");
    //      catch (Exception e) {
    //         .error("   Erreur mise à jour Auth Service: {}", e.getMessage());
    //     
        
    //      {
    //         .updateUserRole(agent.getId(), "AGENT");
    //         .info("   Rôle AGENT mis à jour");
    //      catch (Exception e) {
    //         .warn("   Impossible de mettre à jour le rôle: {}", e.getMessage());
    //     
        
    //     .info("{} assigné à l'agence {} - Référence: {}", agent.getEmail(), agency.getCode(), reference);
        
    //      mapToAssignmentResponse(assignment, agency);
    // 
    
    private UserDTO validateAndGetAgent(AgentAssignmentRequest request) {
        UserDTO agent = null;
        
        if (request.getAgentId() != null && !request.getAgentId().isEmpty()) {
            try {
                log.info("de l'agent par ID: {}", request.getAgentId());
                agent = authServiceClient.getUserById(request.getAgentId());
                
                if (agent == null) {
                    throw new RuntimeException("Aucun utilisateur trouvé avec l'ID: " + request.getAgentId());
                }
                
                log.info("trouvé par ID: {} - Nom: {}", agent.getEmail(), agent.getFirstName());
                
                if (agent.getUserRoleType() == null || !agent.getUserRoleType().equals("AGENT")) {
                    throw new RuntimeException(
                        String.format("L'utilisateur '%s' n'a pas le rôle AGENT. Rôle actuel: %s.",
                            agent.getEmail(), agent.getUserRoleType()));
                }
            } catch (FeignException.NotFound e) {
                throw new RuntimeException("Agent non trouvé dans Auth Service: " + request.getAgentId());
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de la recherche de l'agent: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("L'ID de l'agent est requis");
        }
        
        return agent;
    }
    
    private String generateReference(String providedReference) {
        if (providedReference != null && !providedReference.trim().isEmpty()) {
            return providedReference;
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ASSIGN-" + timestamp + "-" + random;
    }
    
    private LocalDateTime parseAssignmentDate(String assignmentDate) {
        if (assignmentDate != null && !assignmentDate.trim().isEmpty()) {
            try {
                return LocalDateTime.parse(assignmentDate);
            } catch (Exception e) {
                log.warn("de date invalide: {}", assignmentDate);
            }
        }
        return LocalDateTime.now();
    }
    
    public boolean canBeDirector(String userId) {
        try {
            UserDTO user = authServiceClient.getUserById(userId);
            if (user == null) return false;
            Optional<Agency> existingAgency = agencyRepository.findByDirectorId(userId);
            return existingAgency.isEmpty();
        } catch (Exception e) {
            log.error("lors de la vérification: {}", e.getMessage());
            return false;
        }
    }
    
    public UserDTO getDirectorInfo(String directorId) {
        try {
            return authServiceClient.getUserById(directorId);
        } catch (Exception e) {
            log.error("lors de la récupération du directeur: {}", e.getMessage());
            return null;
        }
    }
    
    @Transactional
    public void unassignAgent(String agentId, String unassignedBy, String unassignedByName) {
        log.info("de l'agent: {}", agentId);
        
        AgentAssignment assignment = agentAssignmentRepository.findByAgentIdAndActiveTrue(agentId)
            .orElseThrow(() -> new RuntimeException("Agent non assigné"));
        
        assignment.setActive(false);
        agentAssignmentRepository.save(assignment);
        
        try {
            authServiceClient.updateUserRole(agentId, "AGENT_SANS_AGENCE");
            authServiceClient.updateUserAgency(agentId, null, null);
        } catch (Exception e) {
            log.warn("de mettre à jour le rôle de l'agent: {}", e.getMessage());
        }
        
        log.info("désassigné avec succès");
    }
    
    @Transactional(readOnly = true)
    public List<AgencyResponse> getAllAgencies() {
        return agencyRepository.findAll().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
   public AgencyResponse getAgency(String id) {
    log.info("AGENCY SERVICE - GET AGENCY ===");
    
    if (id == null || id.trim().isEmpty()) {
        log.warn("null ou vide");
        return null;
    }
    
    Optional<Agency> agencyOpt = agencyRepository.findById(id);
    if (agencyOpt.isEmpty()) {
        log.warn("agence trouvée avec ID: {}", id);
        return null;
    }
    
    Agency agency = agencyOpt.get();
    long agentsCount = agentAssignmentRepository.countByAgencyIdAndActiveTrue(agency.getId());
    
    //  les statistiques depuis Account Service
    Long totalClients = null;
    Long totalAccounts = null;
    
    try {
        //  Utiliser AgencyStatsResponse au lieu de AccountStatsResponse
        AgencyStatsResponse stats = accountServiceClient.getAgencyAccountStats(agency.getId());
        if (stats != null) {
            totalClients = stats.getTotalClients();
            totalAccounts = stats.getTotalAccounts();
            log.info("récupérées - Clients: {}, Comptes: {}", totalClients, totalAccounts);
        }
    } catch (Exception e) {
        log.warn("de récupérer les stats depuis Account Service: {}", e.getMessage());
    }
    
    return AgencyResponse.builder()
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
            .totalClients(totalClients)      
            .totalAccounts(totalAccounts)    
            .createdAt(agency.getCreatedAt())
            .updatedAt(agency.getUpdatedAt())
            .build();
}

    @Transactional(readOnly = true)
    public AgencyResponse getAgencyByCode(String code) {
        Agency agency = agencyRepository.findByCode(code)
            .orElseThrow(() -> new AgencyNotFoundException("Agence non trouvée avec le code: " + code));
        return mapToResponse(agency);
    }
    
    @Transactional(readOnly = true)
    public AgencyResponse getAgencyByDirector(String directorId) {
        Agency agency = agencyRepository.findByDirectorId(directorId)
            .orElseThrow(() -> new AgencyNotFoundException("Aucune agence trouvée pour ce directeur"));
        return mapToResponse(agency);
    }
    
    @Transactional(readOnly = true)
    public List<AgentAssignmentResponse> getAllActiveAssignments() {
        return agentAssignmentRepository.findAllByActiveTrue().stream()
            .map(a -> {
                Agency agency = agencyRepository.findById(a.getAgencyId()).orElse(null);
                return mapToAssignmentResponse(a, agency);
            })
            .collect(Collectors.toList());
    }

    @Transactional
    public List<AgentAssignmentResponse> getAllAgencyAgents(String agencyId) {
        Agency agency = agencyRepository.findById(agencyId)
            .orElseThrow(() -> new AgencyNotFoundException("Agence non trouvée: " + agencyId));

        List<AgentAssignment> all = agentAssignmentRepository.findByAgencyId(agencyId);

        // Deduplicate: keep only the most recent assignment per agentId
        Map<String, AgentAssignment> latest = new HashMap<>();
        all.stream()
            .sorted((a, b) -> {
                LocalDateTime ta = a.getAssignedAt() != null ? a.getAssignedAt() : LocalDateTime.MIN;
                LocalDateTime tb = b.getAssignedAt() != null ? b.getAssignedAt() : LocalDateTime.MIN;
                return tb.compareTo(ta);
            })
            .forEach(a -> latest.putIfAbsent(a.getAgentId(), a));

        return latest.values().stream()
                .map(a -> mapToAssignmentResponse(a, agency))
                .collect(Collectors.toList());
    }

    public List<AgentAssignmentResponse> getAgencyAgents(String agencyId) {
        Agency agency = agencyRepository.findById(agencyId)
            .orElseThrow(() -> new AgencyNotFoundException("Agence non trouvée: " + agencyId));

        List<AgentAssignment> assignments = agentAssignmentRepository.findByAgencyIdAndActiveTrue(agencyId);
        List<AgentAssignmentResponse> result = new ArrayList<>();
        for (AgentAssignment assignment : assignments) {
            boolean exists = true;
            try {
                UserDTO user = authServiceClient.getUserById(assignment.getAgentId());
                if (user == null || user.getId() == null) exists = false;
            } catch (Exception e) {
                exists = false;
            }
            if (!exists) {
                assignment.setActive(false);
                agentAssignmentRepository.save(assignment);
                log.info("[CLEANUP] Agent {} supprimé de l'agence {} (introuvable dans auth-service)", assignment.getAgentId(), agencyId);
            } else {
                result.add(mapToAssignmentResponse(assignment, agency));
            }
        }
        return result;
    }
    
    @Transactional(readOnly = true)
    public AgentAssignmentResponse getAgentAssignment(String agentId) {
        AgentAssignment assignment = agentAssignmentRepository.findByAgentIdAndActiveTrue(agentId)
            .orElseThrow(() -> new RuntimeException(" Agent non assigné"));
        
        Agency agency = agencyRepository.findById(assignment.getAgencyId()).orElse(null);
        return mapToAssignmentResponse(assignment, agency);
    }
    
    @Transactional(readOnly = true)
    public boolean validateAgentBelongsToAgency(String agentId, String agencyId) {
        return agentAssignmentRepository.findByAgentIdAndActiveTrue(agentId)
            .map(assignment -> assignment.getAgencyId().equals(agencyId))
            .orElse(false);
    }
    
    @Transactional
    public void toggleAgentStatus(String agentId, String agencyId) {
        List<AgentAssignment> assignments = agentAssignmentRepository
                .findByAgentIdAndAgencyIdOrderByAssignedAtDesc(agentId, agencyId);
        if (assignments.isEmpty()) {
            throw new RuntimeException("Agent non trouvé dans cette agence");
        }
        AgentAssignment assignment = assignments.get(0);
        assignment.setActive(!assignment.isActive());
        agentAssignmentRepository.save(assignment);
        log.info("Statut agent {} basculé à: {}", agentId, assignment.isActive());
    }

    @Transactional(readOnly = true)
    public AgencyStatsResponse getAgencyStats(String agencyId) {
        Agency agency = agencyRepository.findById(agencyId)
            .orElseThrow(() -> new AgencyNotFoundException("Agence non trouvée: " + agencyId));
        
        long totalAgents = agentAssignmentRepository.countByAgencyIdAndActiveTrue(agencyId);
        
        return AgencyStatsResponse.builder()
            .agencyId(agency.getId())
            .agencyCode(agency.getCode())
            .agencyName(agency.getName())
            .totalAgents(totalAgents)
            .activeAgents(totalAgents)
            .totalClients(null)      //  mis à jour par Account Service
            .totalAccounts(null)     //  mis à jour par Account Service
            .totalLoans(null)        //  mis à jour par Loan Service
            .totalOutstanding(null)  //  mis à jour par Loan Service
            .monthlyRepaymentRate(null)  //  mis à jour par Repayment Service
            .build();
    }
    
    @Transactional
    public AgencyResponse toggleAgencyStatus(String agencyId) {
        log.info("status de l'agence: {}", agencyId);
        
        Agency agency = agencyRepository.findById(agencyId)
            .orElseThrow(() -> new AgencyNotFoundException("Agence non trouvée: " + agencyId));
        
        if ("ACTIVE".equals(agency.getStatus())) {
            agency.setStatus("INACTIVE");
        } else {
            agency.setStatus("ACTIVE");
        }
        
        agency.setUpdatedAt(LocalDateTime.now());
        agency = agencyRepository.save(agency);
        
        return mapToResponse(agency);
    }
    
    @Transactional
    public AgencyResponse activateAgency(String agencyId) {
        Agency agency = agencyRepository.findById(agencyId)
            .orElseThrow(() -> new AgencyNotFoundException("Agence non trouvée: " + agencyId));
        
        agency.setStatus("ACTIVE");
        agency.setUpdatedAt(LocalDateTime.now());
        agency = agencyRepository.save(agency);
        
        log.info("{} activée avec succès", agency.getCode());
        return mapToResponse(agency);
    }
    
    @Transactional
    public AgencyResponse deactivateAgency(String agencyId) {
        Agency agency = agencyRepository.findById(agencyId)
            .orElseThrow(() -> new AgencyNotFoundException("Agence non trouvée: " + agencyId));
        
        agency.setStatus("INACTIVE");
        agency.setUpdatedAt(LocalDateTime.now());
        agency = agencyRepository.save(agency);
        
        log.info("{} désactivée avec succès", agency.getCode());
        return mapToResponse(agency);
    }
    
    @Transactional
    public BulkAgentAssignmentResponse assignMultipleAgentsToAgency(
            BulkAgentAssignmentRequest request, String assignedBy, String assignedByName) {
        
        log.info("ASSIGN AGENTS - Service");
        log.info("   Agency ID: {}", request.getAgencyId());
        log.info("   Number of agents: {}", request.getAgents().size());
        
        Agency agency = agencyRepository.findById(request.getAgencyId())
            .orElseThrow(() -> new AgencyNotFoundException("Agence non trouvée: " + request.getAgencyId()));
        
        log.info("   Agence trouvée: {} - {}", agency.getCode(), agency.getName());
        
        List<BulkAgentAssignmentResponse.AssignmentResult> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        
        LocalDateTime assignmentDate = parseAssignmentDate(request.getAssignmentDate());
        
        for (BulkAgentAssignmentRequest.AgentAssignmentItem item : request.getAgents()) {
            try {
                BulkAgentAssignmentResponse.AssignmentResult result = assignSingleAgent(
                    item, agency, assignmentDate, request.getGlobalReason(), 
                    request.getGlobalReference(), assignedBy, assignedByName);
                
                results.add(result);
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                log.error("lors de l'assignation de l'agent {}: {}", item.getAgentId(), e.getMessage());
                results.add(BulkAgentAssignmentResponse.AssignmentResult.builder()
                    .agentId(item.getAgentId())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build());
                failedCount++;
            }
        }
        
        log.info("   Résultat: {} succès, {} échecs", successCount, failedCount);
        
        return BulkAgentAssignmentResponse.builder()
            .agencyId(agency.getId())
            .agencyCode(agency.getCode())
            .agencyName(agency.getName())
            .totalRequested(request.getAgents().size())
            .successCount(successCount)
            .failedCount(failedCount)
            .results(results)
            .build();
    }
    
    private BulkAgentAssignmentResponse.AssignmentResult assignSingleAgent(
            BulkAgentAssignmentRequest.AgentAssignmentItem item,
            Agency agency,
            LocalDateTime assignmentDate,
            String globalReason,
            String globalReference,
            String assignedBy,
            String assignedByName) {
        
        log.info("   Traitement de l'agent: {}", item.getAgentId());
        
        UserDTO agent = null;
        try {
            agent = authServiceClient.getUserById(item.getAgentId());
        } catch (Exception e) {
            log.error("lors de la vérification de l'agent: {}", e.getMessage());
            return BulkAgentAssignmentResponse.AssignmentResult.builder()
                .agentId(item.getAgentId())
                .success(false)
                .errorMessage("Agent non trouvé dans Auth Service")
                .build();
        }
        
        if (agent == null) {
            return BulkAgentAssignmentResponse.AssignmentResult.builder()
                .agentId(item.getAgentId())
                .success(false)
                .errorMessage("Agent non trouvé dans le système")
                .build();
        }
        
        if (agentAssignmentRepository.existsByAgentIdAndActiveTrue(item.getAgentId())) {
            return BulkAgentAssignmentResponse.AssignmentResult.builder()
                .agentId(item.getAgentId())
                .agentEmail(agent.getEmail())
                .agentName(agent.getFirstName() + " " + agent.getLastName())
                .success(false)
                .errorMessage("Agent déjà assigné à une agence")
                .build();
        }
        
        String reason = item.getReason() != null ? item.getReason() : globalReason;
        
        String reference = item.getReference();
        if (reference == null || reference.trim().isEmpty()) {
            reference = globalReference;
        }
        if (reference == null || reference.trim().isEmpty()) {
            reference = generateReference(null);
        }
        
        agentAssignmentRepository.deactivateAgentAssignments(item.getAgentId());
        
        AgentAssignment assignment = AgentAssignment.builder()
            .agentId(agent.getId())
            .agentEmail(agent.getEmail())
            .agentName(agent.getFirstName() + " " + agent.getLastName())
            .agencyId(agency.getId())
            .agencyCode(agency.getCode())
            .role("AGENT")
            .assignedBy(assignedBy)
            .assignedByName(assignedByName)
            .reason(reason)
            .reference(reference)
            .assignedAt(assignmentDate)
            .active(true)
            .build();
        
        assignment = agentAssignmentRepository.save(assignment);
        
        //  À JOUR L'AGENT DANS AUTH SERVICE
        try {
            authServiceClient.updateUserAgencyWithAssigner(
                agent.getId(), agency.getId(), agency.getCode(), assignedBy, assignedByName);
            log.info("   Agent mis à jour dans Auth Service");
        } catch (Exception e) {
            log.error("   Erreur mise à jour Auth Service: {}", e.getMessage());
        }
        
        try {
            authServiceClient.updateUserRole(agent.getId(), "AGENT");
            log.info("   Rôle AGENT mis à jour");
        } catch (Exception e) {
            log.warn("   Impossible de mettre à jour le rôle: {}", e.getMessage());
        }
        
        log.info("   Agent {} assigné avec succès - Référence: {}", agent.getEmail(), reference);
        
        return BulkAgentAssignmentResponse.AssignmentResult.builder()
            .agentId(agent.getId())
            .agentEmail(agent.getEmail())
            .agentName(agent.getFirstName() + " " + agent.getLastName())
            .success(true)
            .reference(reference)
            .reason(reason)
            .assignmentId(assignment.getId())
            .build();
    }
    
    private AgencyResponse mapToResponse(Agency agency) {
        long agentsCount = agentAssignmentRepository.countByAgencyIdAndActiveTrue(agency.getId());
        
        return AgencyResponse.builder()
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
    }
    
    private AgentAssignmentResponse mapToAssignmentResponse(AgentAssignment assignment, Agency agency) {
        return AgentAssignmentResponse.builder()
            .id(assignment.getId())
            .agentId(assignment.getAgentId())
            .agentEmail(assignment.getAgentEmail())
            .agentName(assignment.getAgentName())
            .agencyId(assignment.getAgencyId())
            .agencyCode(assignment.getAgencyCode())
            .agencyName(agency != null ? agency.getName() : null)
            .role(assignment.getRole())
            .assignedBy(assignment.getAssignedBy())
            .reason(assignment.getReason())
            .reference(assignment.getReference())
            .assignedByName(assignment.getAssignedByName())
            .assignedAt(assignment.getAssignedAt())
            .active(assignment.isActive())
            .build();
    }

    /**
 * Assigner un directeur à une agence
 */
@Transactional
public AgencyResponse assignDirectorToAgency(String agencyId, String directorId, String assignedBy, String assignedByName) {
    log.info("ASSIGN DIRECTOR - Service");
    log.info("   Agency ID: {}", agencyId);
    log.info("   Director ID: {}", directorId);
    
    // . Vérifier que l'agence existe
    Agency agency = agencyRepository.findById(agencyId)
        .orElseThrow(() -> new AgencyNotFoundException("Agence non trouvée: " + agencyId));
    
    // . Vérifier que le directeur existe dans Auth Service
    UserDTO director = null;
    try {
        director = authServiceClient.getUserById(directorId);
    } catch (Exception e) {
        log.error("lors de la vérification du directeur: {}", e.getMessage());
        throw new RuntimeException("Impossible de vérifier l'existence du directeur. Service Auth indisponible.");
    }
    
    if (director == null) {
        throw new RuntimeException("Le directeur avec ID '" + directorId + "' n'existe pas dans le système.");
    }
    
    // . Vérifier que le directeur a le rôle DIRECTEUR_AGENCE
    if (director.getUserRoleType() == null || !director.getUserRoleType().equals("DIRECTEUR_AGENCE")) {
        throw new RuntimeException(
            String.format("L'utilisateur '%s' n'a pas le rôle DIRECTEUR_AGENCE. Rôle actuel: %s.",
                director.getEmail(), director.getUserRoleType())
        );
    }
    
    // . Vérifier que le directeur n'est pas déjà assigné à une autre agence
    Optional<Agency> existingAgency = agencyRepository.findByDirectorId(directorId);
    if (existingAgency.isPresent() && !existingAgency.get().getId().equals(agencyId)) {
        throw new RuntimeException(
            String.format("Le directeur %s %s est déjà associé à l'agence %s (%s)", 
                director.getFirstName(), director.getLastName(),
                existingAgency.get().getCode(), existingAgency.get().getName())
        );
    }
    
    // . Mettre à jour l'agence
    agency.setDirectorId(director.getId());
    agency.setDirectorEmail(director.getEmail());
    agency.setDirectorName(director.getFirstName() + " " + director.getLastName());
    agency.setUpdatedAt(LocalDateTime.now());
    agency = agencyRepository.save(agency);
    
    // . Mettre à jour l'utilisateur dans Auth Service
    try {
        authServiceClient.updateUserAgencyWithAssigner(
            director.getId(), agency.getId(), agency.getCode(), assignedBy, assignedByName);
        authServiceClient.updateUserRole(director.getId(), "DIRECTEUR_AGENCE");
        log.info("Directeur mis a jour dans Auth Service");
    } catch (Exception e) {
        log.warn("Impossible de mettre a jour le directeur dans Auth Service: {}", e.getMessage());
    }
    
    log.info("Directeur {} assigné à l'agence {}", director.getEmail(), agency.getCode());
    
    return mapToResponse(agency);
}

/**
 * Désassigner le directeur d'une agence
 */
@Transactional
public void unassignDirectorFromAgency(String agencyId, String unassignedBy, String unassignedByName) {
    log.info("UNASSIGN DIRECTOR - Service");
    log.info("   Agency ID: {}", agencyId);
    
    // . Vérifier que l'agence existe
    Agency agency = agencyRepository.findById(agencyId)
        .orElseThrow(() -> new AgencyNotFoundException("Agence non trouvée: " + agencyId));
    
    // . Récupérer l'ancien directeur
    String oldDirectorId = agency.getDirectorId();
    
    if (oldDirectorId == null) {
        log.warn("directeur associé à l'agence: {}", agencyId);
        return;
    }
    
    // . Enlever le directeur de l'agence
    agency.setDirectorId(null);
    agency.setDirectorEmail(null);
    agency.setDirectorName(null);
    agency.setUpdatedAt(LocalDateTime.now());
    agency = agencyRepository.save(agency);
    
    // . Mettre à jour l'utilisateur dans Auth Service
    try {
        authServiceClient.updateUserAgencyWithAssigner(oldDirectorId, null, null, unassignedBy, unassignedByName);
        log.info("Ancien directeur mis a jour dans Auth Service");
    } catch (Exception e) {
        log.warn("Impossible de mettre a jour l'ancien directeur dans Auth Service: {}", e.getMessage());
    }
    
    log.info("Directeur désassigné de l'agence {}", agency.getCode());
}

/**
 * Récupérer le directeur d'une agence
 */
@Transactional(readOnly = true)
public Map<String, Object> getDirectorByAgency(String agencyId) {
    log.info("GET DIRECTOR - Service - Agency ID: {}", agencyId);
    
    Agency agency = agencyRepository.findById(agencyId)
        .orElseThrow(() -> new AgencyNotFoundException("Agence non trouvée: " + agencyId));
    
    Map<String, Object> response = new HashMap<>();
    
    if (agency.getDirectorId() == null) {
        response.put("exists", false);
        response.put("message", "Aucun directeur associé à cette agence");
        return response;
    }
    
    try {
        UserDTO director = authServiceClient.getUserById(agency.getDirectorId());
        
        response.put("exists", true);
        response.put("directorId", director.getId());
        response.put("directorEmail", director.getEmail());
        response.put("directorFirstName", director.getFirstName());
        response.put("directorLastName", director.getLastName());
        response.put("directorFullName", director.getFirstName() + " " + director.getLastName());
        response.put("agencyId", agency.getId());
        response.put("agencyCode", agency.getCode());
        response.put("agencyName", agency.getName());
        
    } catch (Exception e) {
        log.error("lors de la récupération du directeur: {}", e.getMessage());
        response.put("exists", false);
        response.put("error", "Impossible de récupérer les informations du directeur");
    }
    
    return response;
 }

 @Transactional(readOnly = true)
public AgencyResponse getAgencyByDirectorEmail(String directorEmail) {
    log.info("d'agence pour le directeur email: {}", directorEmail);
    
    Agency agency = agencyRepository.findByDirectorEmail(directorEmail)
            .orElseThrow(() -> new AgencyNotFoundException("Aucune agence trouvée pour ce directeur"));
    
    long agentsCount = agentAssignmentRepository.countByAgencyIdAndActiveTrue(agency.getId());
    
    Long totalClients = null;
    Long totalAccounts = null;
    
    try {
        AgencyStatsResponse stats = accountServiceClient.getAgencyAccountStats(agency.getId());
        if (stats != null) {
            totalClients = stats.getTotalClients();
            totalAccounts = stats.getTotalAccounts();
            log.info("récupérées - Clients: {}, Comptes: {}", totalClients, totalAccounts);
        }
    } catch (Exception e) {
        log.warn("de récupérer les stats depuis Account Service: {}", e.getMessage());
    }
    
    return AgencyResponse.builder()
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
            .totalClients(totalClients)      // ←  rempli
            .totalAccounts(totalAccounts)    // ←  rempli
            .createdAt(agency.getCreatedAt())
            .updatedAt(agency.getUpdatedAt())
            .build();
}
    // .java - Ajoutez cette méthode
   @Transactional(readOnly = true)
   public AgentAssignmentResponse getAgentAssignmentByEmail(String email) {
         log.info("Recherche de l'assignation d'agent par email: {}", email);
    
         //  l'agent par email dans la table agent_assignments
         Optional<AgentAssignment> assignmentOpt = agentAssignmentRepository.findByAgentEmailAndActiveTrue(email);
    
         if (assignmentOpt.isEmpty()) {
            log.warn("assignation trouvée pour l'email: {}", email);
            return null;
         }
    
         AgentAssignment assignment = assignmentOpt.get();
         Agency agency = agencyRepository.findById(assignment.getAgencyId()).orElse(null);
    
         return mapToAssignmentResponse(assignment, agency);
    }


}