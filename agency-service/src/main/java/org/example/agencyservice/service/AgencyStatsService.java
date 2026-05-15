package org.example.agencyservice.service;

import org.example.agencyservice.dto.request.AgencyStatsUpdateRequest;
import org.example.agencyservice.dto.response.AgencyStatsResponse;
import org.example.agencyservice.model.Agency;
import org.example.agencyservice.model.AgencyStats;
import org.example.agencyservice.repository.AgencyRepository;
import org.example.agencyservice.repository.AgencyStatsRepository;
import org.example.agencyservice.repository.AgentAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgencyStatsService {
    
    private final AgencyStatsRepository agencyStatsRepository;
    private final AgencyRepository agencyRepository;
    private final AgentAssignmentRepository agentAssignmentRepository;
    
    /**
     * Initialise les statistiques pour une agence
     */
    @Transactional
    public void initializeStats(String agencyId) {
        if (!agencyStatsRepository.existsById(agencyId)) {
            AgencyStats stats = AgencyStats.builder()
                    .agencyId(agencyId)
                    .totalClients(0L)
                    .totalAccounts(0L)
                    .totalLoans(0L)
                    .totalOutstanding(java.math.BigDecimal.ZERO)
                    .monthlyRepaymentRate(0.0)
                    .activeLoans(0L)
                    .completedLoans(0L)
                    .defaultedLoans(0L)
                    .build();
            agencyStatsRepository.save(stats);
            log.info("initialisées pour l'agence: {}", agencyId);
        }
    }
    
    /**
     * Met à jour les statistiques d'une agence
     */
    @Transactional
    public void updateAgencyStats(String agencyId, AgencyStatsUpdateRequest request) {
        log.info("à jour des stats pour l'agence: {}", agencyId);
        
        //  si nécessaire
        initializeStats(agencyId);
        
        //  à jour les compteurs
        if (request.getNewClients() != null && request.getNewClients() > 0) {
            agencyStatsRepository.incrementTotalClients(agencyId, request.getNewClients());
            log.info("   +{} clients pour l'agence {}", request.getNewClients(), agencyId);
        }
        
        if (request.getNewAccounts() != null && request.getNewAccounts() > 0) {
            agencyStatsRepository.incrementTotalAccounts(agencyId, request.getNewAccounts());
            log.info("   +{} comptes pour l'agence {}", request.getNewAccounts(), agencyId);
        }
    }
    
    /**
     * Récupère les statistiques d'une agence
     */
    @Transactional(readOnly = true)
    public AgencyStatsResponse getAgencyStats(String agencyId) {
        log.info("des statistiques pour l'agence: {}", agencyId);
        
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new RuntimeException("Agence non trouvée: " + agencyId));
        
        long totalAgents = agentAssignmentRepository.countByAgencyIdAndActiveTrue(agencyId);
        
        AgencyStats stats = agencyStatsRepository.findByAgencyId(agencyId)
                .orElse(AgencyStats.builder()
                        .agencyId(agencyId)
                        .totalClients(0L)
                        .totalAccounts(0L)
                        .totalLoans(0L)
                        .totalOutstanding(java.math.BigDecimal.ZERO)
                        .monthlyRepaymentRate(0.0)
                        .build());
        
        return AgencyStatsResponse.builder()
                .agencyId(agency.getId())
                .agencyCode(agency.getCode())
                .agencyName(agency.getName())
                .totalAgents(totalAgents)
                .activeAgents(totalAgents)
                .totalClients(stats.getTotalClients())
                .totalAccounts(stats.getTotalAccounts())
                .totalLoans(stats.getTotalLoans())
                .totalOutstanding(stats.getTotalOutstanding())
                .monthlyRepaymentRate(stats.getMonthlyRepaymentRate())
                .build();
    }

    /**
     * Incrémente le nombre de clients d'une agence
     */
    @Transactional
    public void incrementTotalClients(String agencyId, Long increment) {
        AgencyStats stats = getOrCreateStats(agencyId);
        stats.setTotalClients(stats.getTotalClients() + increment);
        agencyStatsRepository.save(stats);
        log.info("   +{} clients pour l'agence {}, total: {}", increment, agencyId, stats.getTotalClients());
    }
    
    /**
     * Incrémente le nombre de comptes d'une agence
     */
    @Transactional
    public void incrementTotalAccounts(String agencyId, Long increment) {
        AgencyStats stats = getOrCreateStats(agencyId);
        stats.setTotalAccounts(stats.getTotalAccounts() + increment);
        agencyStatsRepository.save(stats);
        log.info("   +{} comptes pour l'agence {}, total: {}", increment, agencyId, stats.getTotalAccounts());
    }
    
    /**
     * Définit le nombre total de clients (valeur absolue)
     */
    @Transactional
    public void setTotalClients(String agencyId, Long totalClients) {
        AgencyStats stats = getOrCreateStats(agencyId);
        stats.setTotalClients(totalClients);
        agencyStatsRepository.save(stats);
        log.info("   Total clients pour l'agence {}: {}", agencyId, totalClients);
    }
    
    /**
     * Définit le nombre total de comptes (valeur absolue)
     */
    @Transactional
    public void setTotalAccounts(String agencyId, Long totalAccounts) {
        AgencyStats stats = getOrCreateStats(agencyId);
        stats.setTotalAccounts(totalAccounts);
        agencyStatsRepository.save(stats);
        log.info("   Total comptes pour l'agence {}: {}", agencyId, totalAccounts);
    }
    
    /**
     * Récupère ou crée les statistiques d'une agence
     */
    private AgencyStats getOrCreateStats(String agencyId) {
        return agencyStatsRepository.findByAgencyId(agencyId)
                .orElseGet(() -> {
                    AgencyStats newStats = AgencyStats.builder()
                            .agencyId(agencyId)
                            .totalClients(0L)
                            .totalAccounts(0L)
                            .totalLoans(0L)
                            .totalOutstanding(BigDecimal.ZERO)
                            .monthlyRepaymentRate(0.0)
                            .build();
                    return agencyStatsRepository.save(newStats);
                });
    }
}