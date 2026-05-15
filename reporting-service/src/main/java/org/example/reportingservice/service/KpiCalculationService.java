package org.example.reportingservice.service;

import org.example.reportingservice.client.ClientServiceClient;
import org.example.reportingservice.client.ClientStats;
import org.example.reportingservice.client.LoanServiceClient;
import org.example.reportingservice.client.LoanStats;
import org.example.reportingservice.client.RepaymentServiceClient;
import org.example.reportingservice.client.RepaymentStats;
import org.example.reportingservice.model.Kpi;
import org.example.reportingservice.repository.KpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KpiCalculationService {
    
    private final LoanServiceClient loanServiceClient;
    private final ClientServiceClient clientServiceClient;
    private final RepaymentServiceClient repaymentServiceClient;
    private final KpiRepository kpiRepository;
    
    /**
     * Récupère le token JWT depuis le contexte Spring Security
     */
    private String getAuthToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof String) {
            String token = (String) authentication.getCredentials();
            if (token != null && !token.startsWith("Bearer ")) {
                return "Bearer " + token;
            }
            return token;
        }
        return null;
    }
    
    @Transactional
    public void updateLoanKpis() {
        log.info("à jour des KPIs de prêts");
        
        String token = getAuthToken();
        if (token == null) {
            log.warn("token trouvé, impossible de mettre à jour les KPIs de prêts");
            return;
        }
        
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0);
            LocalDateTime startOfYear = now.withDayOfYear(1).withHour(0).withMinute(0);
            
            //  les statistiques avec token
            LoanStats monthlyStats = loanServiceClient.getLoanStats(startOfMonth, now, token);
            LoanStats yearlyStats = loanServiceClient.getLoanStats(startOfYear, now, token);
            
            if (monthlyStats != null) {
                // . Nombre total de demandes de prêt
                saveKpi("TOTAL_LOAN_APPLICATIONS", "Nombre total de demandes de prêt",
                    "LOANS", BigDecimal.valueOf(monthlyStats.getTotalApplications() != null ? monthlyStats.getTotalApplications() : 0L),
                    "demandes", startOfMonth, now);
                
                // . Taux d'approbation
                Double approvalRate = monthlyStats.getApprovalRate();
                saveKpi("APPROVAL_RATE", "Taux d'approbation des prêts",
                    "LOANS", BigDecimal.valueOf(approvalRate != null ? approvalRate : 0.0),
                    "%", startOfMonth, now);
                
                // . Taux de défaut
                Double defaultRate = monthlyStats.getDefaultRate();
                saveKpi("DEFAULT_RATE", "Taux de défaut de paiement",
                    "LOANS", BigDecimal.valueOf(defaultRate != null ? defaultRate : 0.0),
                    "%", startOfMonth, now);
                
                // . Montant total décaissé
                BigDecimal totalDisbursed = monthlyStats.getTotalDisbursedAmount();
                saveKpi("TOTAL_DISBURSED_AMOUNT", "Montant total décaissé",
                    "LOANS", totalDisbursed != null ? totalDisbursed : BigDecimal.ZERO,
                    "FCFA", startOfMonth, now);
                
                // . Encours total
                BigDecimal outstanding = monthlyStats.getOutstandingAmount();
                saveKpi("OUTSTANDING_AMOUNT", "Encours total des prêts",
                    "LOANS", outstanding != null ? outstanding : BigDecimal.ZERO,
                    "FCFA", startOfMonth, now);
                
                // . Nombre de prêts actifs
                saveKpi("ACTIVE_LOANS", "Nombre de prêts actifs",
                    "LOANS", BigDecimal.valueOf(monthlyStats.getActiveLoans() != null ? monthlyStats.getActiveLoans() : 0L),
                    "prêts", startOfMonth, now);
                
                // . Taille moyenne des prêts
                BigDecimal averageLoanSize = calculateAverageLoanSize(monthlyStats);
                saveKpi("AVERAGE_LOAN_SIZE", "Taille moyenne des prêts",
                    "LOANS", averageLoanSize, "FCFA", startOfMonth, now);
            }
            
            if (yearlyStats != null) {
                // . Croissance annuelle des prêts
                Double yearlyGrowth = calculateYearlyLoanGrowth(yearlyStats);
                saveKpi("LOAN_GROWTH_YTD", "Croissance des prêts depuis le début de l'année",
                    "LOANS", BigDecimal.valueOf(yearlyGrowth), "%", startOfYear, now);
            }
            
            log.info("de prêts mis à jour avec succès");
            
        } catch (Exception e) {
            log.error("lors de la mise à jour des KPIs de prêts: {}", e.getMessage(), e);
        }
    }
    
    @Transactional
    public void updateRepaymentKpis() {
        log.info("à jour des KPIs de remboursements");
        
        String token = getAuthToken();
        if (token == null) {
            log.warn("token trouvé, impossible de mettre à jour les KPIs de remboursements");
            return;
        }
        
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0);
            LocalDateTime startOfQuarter = now.minusMonths(3).withDayOfMonth(1).withHour(0).withMinute(0);
            
            //  les statistiques avec token
            RepaymentStats monthlyStats = repaymentServiceClient.getRepaymentStats(startOfMonth, now, token);
            RepaymentStats quarterlyStats = repaymentServiceClient.getRepaymentStats(startOfQuarter, now, token);
            
            if (monthlyStats != null) {
                // . Montant total remboursé
                BigDecimal totalRepayments = monthlyStats.getTotalRepayments();
                saveKpi("TOTAL_REPAYMENTS", "Montant total remboursé",
                    "REPAYMENTS", totalRepayments != null ? totalRepayments : BigDecimal.ZERO,
                    "FCFA", startOfMonth, now);
                
                // . Taux de remboursement
                Double repaymentRate = monthlyStats.getRepaymentRate();
                saveKpi("REPAYMENT_RATE", "Taux de remboursement",
                    "REPAYMENTS", BigDecimal.valueOf(repaymentRate != null ? repaymentRate : 0.0),
                    "%", startOfMonth, now);
                
                // . Montant en retard
                BigDecimal overdueAmount = monthlyStats.getOverdueAmount();
                saveKpi("OVERDUE_AMOUNT", "Montant des paiements en retard",
                    "REPAYMENTS", overdueAmount != null ? overdueAmount : BigDecimal.ZERO,
                    "FCFA", startOfMonth, now);
                
                // . Nombre de retards
                Long overdueCount = monthlyStats.getOverdueCount();
                saveKpi("OVERDUE_COUNT", "Nombre de paiements en retard",
                    "REPAYMENTS", BigDecimal.valueOf(overdueCount != null ? overdueCount : 0L),
                    "retards", startOfMonth, now);
                
                // . Nombre de transactions
                Long totalTransactions = monthlyStats.getTotalTransactions();
                saveKpi("TOTAL_TRANSACTIONS", "Nombre total de transactions",
                    "REPAYMENTS", BigDecimal.valueOf(totalTransactions != null ? totalTransactions : 0L),
                    "transactions", startOfMonth, now);
            }
            
            if (quarterlyStats != null) {
                // . Ratio impayés sur 90 jours
                BigDecimal overdue90Days = quarterlyStats.getOverdueAmount();
                BigDecimal totalRepayments90 = quarterlyStats.getTotalRepayments();
                Double overdueRatio = calculateOverdueRatio(overdue90Days, totalRepayments90);
                saveKpi("OVERDUE_90_DAYS_RATIO", "Ratio des impayés sur 90 jours",
                    "REPAYMENTS", BigDecimal.valueOf(overdueRatio), "%", startOfQuarter, now);
            }
            
            log.info("de remboursements mis à jour avec succès");
            
        } catch (Exception e) {
            log.error("lors de la mise à jour des KPIs de remboursements: {}", e.getMessage(), e);
        }
    }
    
    @Transactional
    public void updateClientKpis() {
        log.info("à jour des KPIs clients");
        
        String token = getAuthToken();
        if (token == null) {
            log.warn("token trouvé, impossible de mettre à jour les KPIs clients");
            return;
        }
        
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0);
            LocalDateTime startOfYear = now.withDayOfYear(1).withHour(0).withMinute(0);
            
            //  les statistiques avec token
            ClientStats clientStats = clientServiceClient.getClientStats(token);
            
            if (clientStats != null) {
                // . Nombre total de clients
                Long totalClients = clientStats.getTotalClients();
                saveKpi("TOTAL_CLIENTS", "Nombre total de clients",
                    "CLIENTS", BigDecimal.valueOf(totalClients != null ? totalClients : 0L),
                    "clients", startOfYear, now);
                
                // . Nouveaux clients du mois
                Long newClients = clientStats.getNewClientsThisMonth();
                saveKpi("NEW_CLIENTS_MONTH", "Nouveaux clients du mois",
                    "CLIENTS", BigDecimal.valueOf(newClients != null ? newClients : 0L),
                    "clients", startOfMonth, now);
                
                // . Clients actifs
                Long activeClients = clientStats.getActiveClients();
                saveKpi("ACTIVE_CLIENTS", "Nombre de clients actifs",
                    "CLIENTS", BigDecimal.valueOf(activeClients != null ? activeClients : 0L),
                    "clients", startOfYear, now);
                
                // . Taux de croissance des clients
                Double growthRate = clientStats.getClientGrowthRate();
                saveKpi("CLIENT_GROWTH_RATE", "Taux de croissance des clients",
                    "CLIENTS", BigDecimal.valueOf(growthRate != null ? growthRate : 0.0),
                    "%", startOfMonth, now);
                
                // . Taux d'engagement des clients
                Double engagementRate = calculateEngagementRate(activeClients, totalClients);
                saveKpi("CLIENT_ENGAGEMENT_RATE", "Taux d'engagement des clients (clients actifs / total)",
                    "CLIENTS", BigDecimal.valueOf(engagementRate), "%", startOfYear, now);
            }
            
            log.info("clients mis à jour avec succès");
            
        } catch (Exception e) {
            log.error("lors de la mise à jour des KPIs clients: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Met à jour tous les KPIs
     */
    @Transactional
    public void updateAllKpis() {
        log.info("à jour de tous les KPIs");
        updateLoanKpis();
        updateRepaymentKpis();
        updateClientKpis();
        log.info("les KPIs ont été mis à jour");
    }
    
    private void saveKpi(String name, String description, String category, 
                         BigDecimal value, String unit, LocalDateTime periodStart, LocalDateTime periodEnd) {
        try {
            Kpi kpi = Kpi.builder()
                .name(name)
                .description(description)
                .category(category)
                .value(value)
                .unit(unit)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .calculatedAt(LocalDateTime.now())
                .calculatedBy("SYSTEM")
                .metadata(buildMetadata(periodStart, periodEnd))
                .build();
            
            kpiRepository.save(kpi);
            log.debug("sauvegardé: {} = {}", name, value);
            
        } catch (Exception e) {
            log.error("lors de la sauvegarde du KPI {}: {}", name, e.getMessage());
        }
    }
    
    private BigDecimal calculateAverageLoanSize(LoanStats stats) {
        if (stats == null) return BigDecimal.ZERO;
        
        Long disbursedLoans = stats.getDisbursedLoans();
        BigDecimal totalDisbursed = stats.getTotalDisbursedAmount();
        
        if (disbursedLoans == null || disbursedLoans == 0 || totalDisbursed == null) {
            return BigDecimal.ZERO;
        }
        
        return totalDisbursed.divide(BigDecimal.valueOf(disbursedLoans), 0, RoundingMode.HALF_UP);
    }
    
    private Double calculateYearlyLoanGrowth(LoanStats yearlyStats) {
        //  simplifié - à améliorer avec les données historiques
        if (yearlyStats == null) return 0.0;
        
        Long totalApplications = yearlyStats.getTotalApplications();
        if (totalApplications == null || totalApplications == 0) return 0.0;
        
        //  comparer avec l'année précédente
        //  l'instant, retourne une valeur par défaut
        return 12.5;
    }
    
    private Double calculateOverdueRatio(BigDecimal overdueAmount, BigDecimal totalRepayments) {
        if (overdueAmount == null || totalRepayments == null || totalRepayments.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return overdueAmount.divide(totalRepayments, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .doubleValue();
    }
    
    private Double calculateEngagementRate(Long activeClients, Long totalClients) {
        if (activeClients == null || totalClients == null || totalClients == 0) {
            return 0.0;
        }
        return (double) activeClients / totalClients * 100;
    }
    
    private String buildMetadata(LocalDateTime periodStart, LocalDateTime periodEnd) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("periodStart", periodStart != null ? periodStart.toString() : "");
        metadata.put("periodEnd", periodEnd != null ? periodEnd.toString() : "");
        metadata.put("calculatedBy", "SYSTEM");
        metadata.put("version", "1.0");
        
        //  en JSON simple
        return "{\"periodStart\":\"" + (periodStart != null ? periodStart.toString() : "") + 
               "\",\"periodEnd\":\"" + (periodEnd != null ? periodEnd.toString() : "") + 
               "\",\"calculatedBy\":\"SYSTEM\",\"version\":\"1.0\"}";
    }
}