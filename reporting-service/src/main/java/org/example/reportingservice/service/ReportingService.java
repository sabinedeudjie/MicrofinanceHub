package org.example.reportingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import org.example.reportingservice.client.ClientServiceClient;
import org.example.reportingservice.client.ClientStats;
import org.example.reportingservice.client.LoanServiceClient;
import org.example.reportingservice.client.LoanStats;
import org.example.reportingservice.client.PortfolioStats;
import org.example.reportingservice.client.RepaymentServiceClient;
import org.example.reportingservice.client.RepaymentStats;
import org.example.reportingservice.dto.ClientInfo;
import org.example.reportingservice.dto.reponse.ClientDashboardResponse;
import org.example.reportingservice.dto.reponse.DashboardResponse;
import org.example.reportingservice.dto.reponse.KpiResponse;
import org.example.reportingservice.dto.reponse.LoanReportResponse;
import org.example.reportingservice.dto.reponse.LoanResponse;
import org.example.reportingservice.model.Kpi;
import org.example.reportingservice.repository.KpiRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingService {
    
    private final LoanServiceClient loanServiceClient;
    private final ClientServiceClient clientServiceClient;
    private final RepaymentServiceClient repaymentServiceClient;
    private final KpiRepository kpiRepository;
    
    public DashboardResponse getDashboard() {
        log.info("GÉNÉRATION DU TABLEAU DE BORD");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0);
        
        log.info("du {} au {}", startOfMonth, now);
        
        try {
            String token = getAuthToken();
            if (token == null) {
                log.warn("token trouvé, impossible d'appeler les services");
                return getEmptyDashboard(now);
            }
            
            String userRole = getCurrentUserRole();
            String userEmail = getCurrentUserEmail();
            
            ClientStats clientStats;
            LoanStats loanStats;
            RepaymentStats repaymentStats;
            PortfolioStats portfolioStats;
            
            if ("AGENT".equals(userRole) && userEmail != null) {
                log.info("AGENT - Filtrage pour: {}", userEmail);
                
                String agentId = getAgentIdByEmail(userEmail, token);
                
                if (agentId != null) {
                    try {
                        List<ClientInfo> agentClients = clientServiceClient.getMyClients(agentId, token);
                        
                        if (agentClients != null && !agentClients.isEmpty()) {
                            List<String> clientIds = agentClients.stream()
                                .map(ClientInfo::getId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                            
                            clientStats = clientServiceClient.getMyClientsStats(agentId, token);
                            
                            if (!clientIds.isEmpty()) {
                                loanStats = loanServiceClient.getLoanStatsForClients(clientIds, startOfMonth, now, token);
                                repaymentStats = repaymentServiceClient.getRepaymentStatsForClients(clientIds, startOfMonth, now, token);
                                portfolioStats = loanServiceClient.getPortfolioStatsForClients(clientIds, token);
                            } else {
                                clientStats = createEmptyClientStats();
                                loanStats = createEmptyLoanStats();
                                repaymentStats = createEmptyRepaymentStats();
                                portfolioStats = createEmptyPortfolioStats();
                            }
                        } else {
                            clientStats = createEmptyClientStats();
                            loanStats = createEmptyLoanStats();
                            repaymentStats = createEmptyRepaymentStats();
                            portfolioStats = createEmptyPortfolioStats();
                        }
                    } catch (Exception e) {
                        log.error("{}", e.getMessage());
                        clientStats = createEmptyClientStats();
                        loanStats = createEmptyLoanStats();
                        repaymentStats = createEmptyRepaymentStats();
                        portfolioStats = createEmptyPortfolioStats();
                    }
                } else {
                    clientStats = createEmptyClientStats();
                    loanStats = createEmptyLoanStats();
                    repaymentStats = createEmptyRepaymentStats();
                    portfolioStats = createEmptyPortfolioStats();
                }
            } else {
                //  - Passer token
                clientStats = clientServiceClient.getClientStats(token);
                loanStats = loanServiceClient.getLoanStats(startOfMonth, now, token);
                repaymentStats = repaymentServiceClient.getRepaymentStats(startOfMonth, now, token);
                portfolioStats = loanServiceClient.getPortfolioStats(token);
            }
            
            //  1: Calculer correctement le taux de remboursement
            double repaymentRate = 0.0;
            if (loanStats != null && repaymentStats != null) {
                BigDecimal totalDisbursed = loanStats.getTotalDisbursedAmount();
                BigDecimal totalRepaid = repaymentStats.getTotalRepayments();
                
                if (totalDisbursed != null && totalDisbursed.compareTo(BigDecimal.ZERO) > 0 && totalRepaid != null) {
                    repaymentRate = totalRepaid.divide(totalDisbursed, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
                    log.info("de remboursement calculé: {}% (Décaissé: {}, Remboursé: {})", 
                        repaymentRate, totalDisbursed, totalRepaid);
                }
            }
            
            //  du Dashboard
            Long totalClients = (clientStats != null && clientStats.getTotalClients() != null) ? clientStats.getTotalClients() : 0L;
            Long activeLoans = (loanStats != null && loanStats.getActiveLoans() != null) ? loanStats.getActiveLoans() : 0L;
            BigDecimal totalOutstanding = (portfolioStats != null && portfolioStats.getTotalOutstanding() != null) ? portfolioStats.getTotalOutstanding() : BigDecimal.ZERO;
            BigDecimal totalRepaymentsThisMonth = (repaymentStats != null && repaymentStats.getTotalRepayments() != null) ? repaymentStats.getTotalRepayments() : BigDecimal.ZERO;
            Double clientGrowthRate = (clientStats != null && clientStats.getClientGrowthRate() != null) ? clientStats.getClientGrowthRate() : 0.0;
            
            double defaultRate = calculateDefaultRate(loanStats);
            
            DashboardResponse response = DashboardResponse.builder()
                .timestamp(now)
                .totalClients(totalClients)
                .activeLoans(activeLoans)
                .totalOutstanding(totalOutstanding)
                .totalRepaymentsThisMonth(totalRepaymentsThisMonth)
                .repaymentRate(repaymentRate)
                .defaultRate(defaultRate)
                .clientGrowthRate(clientGrowthRate)
                .loanGrowthRate(calculateLoanGrowthRate(loanStats, token))
                .loansOverdue30Days(countOverdueLoans(30, token))
                .loansOverdue90Days(countOverdueLoans(90, token))
                .pendingApplications(calculatePendingApplications(token))
                .build();
            
            log.info("de bord généré avec succès");
            return response;
            
        } catch (Exception e) {
            log.error("lors de la génération du dashboard: {}", e.getMessage(), e);
            return getEmptyDashboard(now);
        }
    }
    
    // 
    //  CORRIGÉES
    // 
    
    /**
     * Calcule le nombre de demandes en attente
     */
    private Long calculatePendingApplications(String token) {
        try {
            if (token == null) return 0L;
            Page<?> pendingPage = loanServiceClient.getPendingApplications(0, 100, token);
            if (pendingPage != null && pendingPage.getContent() != null) {
                return (long) pendingPage.getContent().size();
            }
            return 0L;
        } catch (Exception e) {
            log.error("calcul demandes en attente: {}", e.getMessage());
            return 0L;
        }
    }
    
    /**
     * Compte les prêts en retard
     */
    private Long countOverdueLoans(int days, String token) {
        try {
            if (token == null) return 0L;
            //  avec Loan Service si disponible
            return 0L;
        } catch (Exception e) {
            log.error("comptage prêts en retard: {}", e.getMessage());
            return 0L;
        }
    }
    
    /**
     * Calcule le taux de croissance des prêts
     */
   private double calculateLoanGrowthRate(LoanStats currentStats, String token) {
    if (currentStats == null) return 0.0;
    
    try {
        if (token == null) return 0.0;
        
        LocalDateTime lastMonthStart = LocalDateTime.now().minusMonths(1).withDayOfMonth(1);
        LocalDateTime lastMonthEnd = LocalDateTime.now().minusMonths(1).withDayOfMonth(1).plusMonths(1).minusSeconds(1);
        
        LoanStats lastMonthStats = loanServiceClient.getLoanStats(lastMonthStart, lastMonthEnd, token);
        
        if (lastMonthStats == null) {
            log.info("donnée pour le mois dernier, croissance = 0%");
            return 0.0;
        }
        
        BigDecimal currentAmount = currentStats.getTotalDisbursedAmount();
        BigDecimal lastMonthAmount = lastMonthStats.getTotalDisbursedAmount();
        
        if (currentAmount == null || lastMonthAmount == null || lastMonthAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        
        double growth = currentAmount.subtract(lastMonthAmount)
            .divide(lastMonthAmount, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .doubleValue();
        
        log.info("des prêts: {}% (Mois dernier: {}, Ce mois: {})", 
            growth, lastMonthAmount, currentAmount);
        
        return growth;
        
    } catch (Exception e) {
        log.error("calcul croissance: {}", e.getMessage());
        return 0.0;
    }
}
    
    /**
     * Calcule le taux de défaut
     */
    private double calculateDefaultRate(LoanStats loanStats) {
        if (loanStats == null) return 0.0;
        Long totalApplications = loanStats.getTotalApplications();
        if (totalApplications == null || totalApplications == 0) return 0.0;
        Long defaultedLoans = loanStats.getDefaultedLoans();
        if (defaultedLoans == null) return 0.0;
        return (double) defaultedLoans / totalApplications * 100;
    }
    
    // 
    // 
    // 
    
    public List<KpiResponse> getKpis(String category) {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║              RÉCUPÉRATION DES KPIS                             ║");
        log.info("╠════════════════════════════════════════════════════════════════╣");
        log.info("║ Catégorie demandée: {}", category != null ? category : "TOUTES");
        log.info("╚════════════════════════════════════════════════════════════════╝");
        
        String token = getAuthToken();
        
        List<KpiResponse> allKpis = new ArrayList<>();
        
        // . Ajouter les KPIs généraux
        allKpis.addAll(generateGeneralKpis(token));
        
        // . Si la catégorie est un CLIENT_xxx, ajouter les KPIs spécifiques du client
        if (category != null && category.startsWith("CLIENT_")) {
            String clientId = category.substring("CLIENT_".length());
            log.info("des KPIs pour le client: {}", clientId);
            allKpis.addAll(generateClientKpis(clientId, token));
        }
        
        // . Filtrer par catégorie si demandé
        if (category != null && !category.isEmpty() && !"TOUTES".equalsIgnoreCase(category)) {
            List<KpiResponse> filteredKpis = allKpis.stream()
                .filter(kpi -> category.equalsIgnoreCase(kpi.getCategory()))
                .collect(Collectors.toList());
            
            log.info("par catégorie '{}': {} KPIs sur {} conservés", 
                category, filteredKpis.size(), allKpis.size());
            return filteredKpis;
        }
        
        log.info("de tous les {} KPIs", allKpis.size());
        return allKpis;
    }
    
    private List<KpiResponse> generateGeneralKpis(String token) {
        List<KpiResponse> kpis = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0);
        
        try {
            if (token == null) return kpis;
            
            LoanStats loanStats = loanServiceClient.getLoanStats(startOfMonth, now, token);
            PortfolioStats portfolioStats = loanServiceClient.getPortfolioStats(token);
            ClientStats clientStats = clientServiceClient.getClientStats(token);
            
            if (loanStats != null) {
                kpis.add(createKpiResponse("ACTIVE_LOANS", "Prêts actifs", 
                    String.valueOf(loanStats.getActiveLoans()), "prêts", "LOANS"));
                kpis.add(createKpiResponse("TOTAL_APPLICATIONS", "Demandes totales", 
                    String.valueOf(loanStats.getTotalApplications()), "demandes", "LOANS"));
                kpis.add(createKpiResponse("APPROVAL_RATE", "Taux d'approbation", 
                    String.format("%.1f%%", loanStats.getApprovalRate()), "%", "LOANS"));
                kpis.add(createKpiResponse("TOTAL_DISBURSED", "Montant total décaissé", 
                    String.format("%,.0f FCFA", loanStats.getTotalDisbursedAmount()), "FCFA", "FINANCIAL"));
                kpis.add(createKpiResponse("TOTAL_REPAID", "Montant total remboursé", 
                    String.format("%,.0f FCFA", loanStats.getTotalRepaidAmount()), "FCFA", "FINANCIAL"));
            }
            
            if (portfolioStats != null) {
                kpis.add(createKpiResponse("OUTSTANDING", "Encours total", 
                    String.format("%,.0f FCFA", portfolioStats.getTotalOutstanding()), "FCFA", "FINANCIAL"));
            }
            
            if (clientStats != null) {
                kpis.add(createKpiResponse("TOTAL_CLIENTS", "Nombre total de clients", 
                    String.valueOf(clientStats.getTotalClients()), "clients", "GENERAL"));
                kpis.add(createKpiResponse("NEW_CLIENTS", "Nouveaux clients (mois)", 
                    String.valueOf(clientStats.getNewClientsThisMonth()), "clients", "GENERAL"));
                kpis.add(createKpiResponse("CLIENT_GROWTH_RATE", "Taux de croissance clients", 
                    String.format("%.1f%%", clientStats.getClientGrowthRate()), "%", "GENERAL"));
            }
            
        } catch (Exception e) {
            log.error("génération KPIs généraux: {}", e.getMessage());
        }
        
        return kpis;
    }
    
    private List<KpiResponse> generateClientKpis(String clientId, String token) {
        List<KpiResponse> kpis = new ArrayList<>();
        
        try {
            if (token == null) return kpis;
            
            List<LoanResponse> clientLoans = loanServiceClient.getClientLoans(clientId, token);
            
            if (clientLoans != null && !clientLoans.isEmpty()) {
                BigDecimal totalBorrowed = clientLoans.stream()
                    .map(LoanResponse::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal totalRepaid = clientLoans.stream()
                    .map(l -> {
                        if (l.getTotalRepayment() != null && l.getRemainingBalance() != null) {
                            return l.getTotalRepayment().subtract(l.getRemainingBalance());
                        }
                        return BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal remainingBalance = clientLoans.stream()
                    .map(LoanResponse::getRemainingBalance)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                int activeLoans = (int) clientLoans.stream()
                    .filter(l -> "ACTIVE".equals(l.getStatus()))
                    .count();
                
                double repaymentRate = totalBorrowed.compareTo(BigDecimal.ZERO) > 0 ?
                    totalRepaid.divide(totalBorrowed, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;
                
                kpis.add(createKpiResponse("CLIENT_TOTAL_BORROWED", "Montant total emprunté",
                    formatCurrency(totalBorrowed), "FCFA", "CLIENT_" + clientId));
                kpis.add(createKpiResponse("CLIENT_TOTAL_REPAID", "Montant total remboursé",
                    formatCurrency(totalRepaid), "FCFA", "CLIENT_" + clientId));
                kpis.add(createKpiResponse("CLIENT_REMAINING_BALANCE", "Solde restant dû",
                    formatCurrency(remainingBalance), "FCFA", "CLIENT_" + clientId));
                kpis.add(createKpiResponse("CLIENT_ACTIVE_LOANS", "Nombre de prêts actifs",
                    String.valueOf(activeLoans), "prêts", "CLIENT_" + clientId));
                kpis.add(createKpiResponse("CLIENT_REPAYMENT_RATE", "Taux de remboursement",
                    String.format("%.1f%%", repaymentRate), "%", "CLIENT_" + clientId));
                
                //  échéance
                LocalDateTime nextPaymentDate = null;
                BigDecimal nextPaymentAmount = BigDecimal.ZERO;
                
                for (LoanResponse loan : clientLoans) {
                    if ("ACTIVE".equals(loan.getStatus()) && loan.getNextPaymentDate() != null) {
                        if (nextPaymentDate == null || loan.getNextPaymentDate().isBefore(nextPaymentDate)) {
                            nextPaymentDate = loan.getNextPaymentDate();
                            nextPaymentAmount = loan.getMonthlyPayment();
                        }
                    }
                }
                
                if (nextPaymentDate != null) {
                    kpis.add(createKpiResponse("CLIENT_NEXT_PAYMENT_DATE", "Prochaine échéance",
                        nextPaymentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), "date", "CLIENT_" + clientId));
                    kpis.add(createKpiResponse("CLIENT_NEXT_PAYMENT_AMOUNT", "Montant de la prochaine échéance",
                        formatCurrency(nextPaymentAmount), "FCFA", "CLIENT_" + clientId));
                }
            } else {
                kpis.add(createKpiResponse("CLIENT_NO_LOANS", "Aucun prêt trouvé",
                    "0", "", "CLIENT_" + clientId));
            }
        } catch (Exception e) {
            log.error("génération KPIs client {}: {}", clientId, e.getMessage());
            kpis.add(createKpiResponse("CLIENT_ERROR", "Erreur de chargement",
                e.getMessage(), "", "CLIENT_" + clientId));
        }
        
        return kpis;
    }
    
    // 
    //  DASHBOARD
    // 
    
    public ClientDashboardResponse getClientKpis(String clientId) {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║         RÉCUPÉRATION DES KPIS POUR LE CLIENT                   ║");
        log.info("╠════════════════════════════════════════════════════════════════╣");
        log.info("║ Client ID: {}", clientId);
        log.info("╚════════════════════════════════════════════════════════════════╝");
        
        String token = getAuthToken();
        
        try {
            if (token == null) return getEmptyClientDashboard();
            
            List<LoanResponse> clientLoans = loanServiceClient.getClientLoans(clientId, token);
            
            if (clientLoans != null && !clientLoans.isEmpty()) {
                int totalLoans = clientLoans.size();
                BigDecimal totalBorrowed = clientLoans.stream()
                    .map(LoanResponse::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal totalRepaid = clientLoans.stream()
                    .map(l -> {
                        if (l.getTotalRepayment() != null && l.getRemainingBalance() != null) {
                            return l.getTotalRepayment().subtract(l.getRemainingBalance());
                        }
                        return BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal remainingBalance = clientLoans.stream()
                    .map(LoanResponse::getRemainingBalance)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                double repaymentRate = totalBorrowed.compareTo(BigDecimal.ZERO) > 0 ?
                    totalRepaid.divide(totalBorrowed, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;
                
                LocalDateTime nextPaymentDate = null;
                BigDecimal nextPaymentAmount = BigDecimal.ZERO;
                
                for (LoanResponse loan : clientLoans) {
                    if ("ACTIVE".equals(loan.getStatus()) && loan.getNextPaymentDate() != null) {
                        if (nextPaymentDate == null || loan.getNextPaymentDate().isBefore(nextPaymentDate)) {
                            nextPaymentDate = loan.getNextPaymentDate();
                            nextPaymentAmount = loan.getMonthlyPayment();
                        }
                    }
                }
                
                List<ClientDashboardResponse.LoanSummary> loanSummaries = clientLoans.stream()
                    .map(loan -> ClientDashboardResponse.LoanSummary.builder()
                        .loanNumber(loan.getLoanNumber())
                        .amount(loan.getAmount())
                        .remainingBalance(loan.getRemainingBalance())
                        .status(loan.getStatus())
                        .nextPaymentDate(loan.getNextPaymentDate())
                        .nextPaymentAmount(loan.getMonthlyPayment())
                        .build())
                    .collect(Collectors.toList());
                
                return ClientDashboardResponse.builder()
                    .totalLoans(totalLoans)
                    .totalBorrowed(totalBorrowed)
                    .totalRepaid(totalRepaid)
                    .remainingBalance(remainingBalance)
                    .repaymentRate(repaymentRate)
                    .nextPaymentDate(nextPaymentDate)
                    .nextPaymentAmount(nextPaymentAmount)
                    .loans(loanSummaries)
                    .build();
            }
            
            return getEmptyClientDashboard();
            
        } catch (Exception e) {
            log.error("{}", e.getMessage(), e);
            return getEmptyClientDashboard();
        }
    }
    
    // 
    // 
    // 
    
    public LoanReportResponse generateLoanReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("du rapport de prêts du {} au {}", startDate, endDate);
        
        String token = getAuthToken();
        
        try {
            if (token == null) return getEmptyLoanReport(startDate, endDate);
            
            LoanStats loanStats = loanServiceClient.getLoanStats(startDate, endDate, token);
            
            if (loanStats == null) {
                return getEmptyLoanReport(startDate, endDate);
            }
            
            return LoanReportResponse.builder()
                .periodStart(startDate)
                .periodEnd(endDate)
                .totalApplications(loanStats.getTotalApplications())
                .approvedApplications(loanStats.getApprovedApplications())
                .rejectedApplications(loanStats.getRejectedApplications())
                .disbursedLoans(loanStats.getDisbursedLoans())
                .activeLoans(loanStats.getActiveLoans())
                .completedLoans(loanStats.getCompletedLoans())
                .defaultedLoans(loanStats.getDefaultedLoans())
                .totalDisbursedAmount(loanStats.getTotalDisbursedAmount())
                .totalRepaidAmount(loanStats.getTotalRepaidAmount())
                .outstandingAmount(loanStats.getOutstandingAmount())
                .averageLoanSize(calculateAverageLoanSize(loanStats))
                .approvalRate(loanStats.getApprovalRate())
                .defaultRate(loanStats.getDefaultRate())
                .recoveryRate(loanStats.getRecoveryRate())
                .monthlyTrends(new ArrayList<>())
                .productBreakdown(new ArrayList<>())
                .build();
                
        } catch (Exception e) {
            log.error("lors de la génération du rapport: {}", e.getMessage());
            return getEmptyLoanReport(startDate, endDate);
        }
    }
    
    // 
    //  UTILITAIRES
    // 
    
    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            String authority = auth.getAuthorities().iterator().next().getAuthority();
            if (authority.startsWith("ROLE_")) {
                return authority.substring(5);
            }
            return authority;
        }
        return null;
    }
    
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
    
    private String getAgentIdByEmail(String email, String token) {
        try {
            if (token == null) return email;
            ClientInfo agentInfo = clientServiceClient.getClientInfoByEmail(email, token);
            return agentInfo != null ? agentInfo.getId() : email;
        } catch (Exception e) {
            log.error("récupération agent ID: {}", e.getMessage());
            return email;
        }
    }
    
    private KpiResponse createKpiResponse(String name, String description, String value, String unit, String category) {
        return KpiResponse.builder()
            .id(java.util.UUID.randomUUID().toString())
            .name(name)
            .description(description)
            .category(category)
            .value(value)
            .unit(unit)
            .periodStart(LocalDateTime.now().minusDays(30))
            .periodEnd(LocalDateTime.now())
            .calculatedAt(LocalDateTime.now())
            .build();
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 FCFA";
        return String.format("%,.0f FCFA", amount);
    }
    
    private BigDecimal calculateAverageLoanSize(LoanStats loanStats) {
        if (loanStats == null) return BigDecimal.ZERO;
        Long disbursedLoans = loanStats.getDisbursedLoans();
        BigDecimal totalDisbursedAmount = loanStats.getTotalDisbursedAmount();
        if (disbursedLoans == null || disbursedLoans == 0 || totalDisbursedAmount == null) return BigDecimal.ZERO;
        return totalDisbursedAmount.divide(BigDecimal.valueOf(disbursedLoans), 2, RoundingMode.HALF_UP);
    }
    
    private DashboardResponse getEmptyDashboard(LocalDateTime now) {
        return DashboardResponse.builder()
            .timestamp(now)
            .totalClients(0L).activeLoans(0L).totalOutstanding(BigDecimal.ZERO)
            .totalRepaymentsThisMonth(BigDecimal.ZERO).repaymentRate(0.0).defaultRate(0.0)
            .clientGrowthRate(0.0).loanGrowthRate(0.0).loansOverdue30Days(0L)
            .loansOverdue90Days(0L).pendingApplications(0L)
            .build();
    }
    
    private LoanReportResponse getEmptyLoanReport(LocalDateTime startDate, LocalDateTime endDate) {
        return LoanReportResponse.builder()
            .periodStart(startDate).periodEnd(endDate)
            .totalApplications(0L).approvedApplications(0L).rejectedApplications(0L)
            .disbursedLoans(0L).activeLoans(0L).completedLoans(0L).defaultedLoans(0L)
            .totalDisbursedAmount(BigDecimal.ZERO).totalRepaidAmount(BigDecimal.ZERO)
            .outstandingAmount(BigDecimal.ZERO).averageLoanSize(BigDecimal.ZERO)
            .approvalRate(0.0).defaultRate(0.0).recoveryRate(0.0)
            .monthlyTrends(new ArrayList<>()).productBreakdown(new ArrayList<>())
            .build();
    }
    
    private ClientDashboardResponse getEmptyClientDashboard() {
        return ClientDashboardResponse.builder()
            .totalLoans(0).totalBorrowed(BigDecimal.ZERO).totalRepaid(BigDecimal.ZERO)
            .remainingBalance(BigDecimal.ZERO).repaymentRate(0.0).loans(new ArrayList<>())
            .build();
    }
    
    private ClientStats createEmptyClientStats() {
        return ClientStats.builder().totalClients(0L).activeClients(0L)
            .newClientsThisMonth(0L).clientGrowthRate(0.0).build();
    }
    
    private LoanStats createEmptyLoanStats() {
        return LoanStats.builder().totalApplications(0L).approvedApplications(0L)
            .rejectedApplications(0L).disbursedLoans(0L).activeLoans(0L)
            .completedLoans(0L).defaultedLoans(0L).totalDisbursedAmount(BigDecimal.ZERO)
            .totalRepaidAmount(BigDecimal.ZERO).outstandingAmount(BigDecimal.ZERO)
            .approvalRate(0.0).defaultRate(0.0).recoveryRate(0.0).build();
    }
    
    private RepaymentStats createEmptyRepaymentStats() {
        return RepaymentStats.builder().totalRepayments(BigDecimal.ZERO).totalTransactions(0L)
            .overdueAmount(BigDecimal.ZERO).overdueCount(0L).repaymentRate(0.0).build();
    }
    
    private PortfolioStats createEmptyPortfolioStats() {
        return PortfolioStats.builder().totalOutstanding(BigDecimal.ZERO)
            .atRisk30Days(BigDecimal.ZERO).atRisk90Days(BigDecimal.ZERO).recoveryRate(0.0).build();
    }
    
    private KpiResponse mapToKpiResponse(Kpi kpi) {
        return KpiResponse.builder()
            .id(kpi.getId()).name(kpi.getName()).description(kpi.getDescription())
            .category(kpi.getCategory()).value(kpi.getValue() != null ? kpi.getValue().toString() : "0")
            .unit(kpi.getUnit()).periodStart(kpi.getPeriodStart()).periodEnd(kpi.getPeriodEnd())
            .calculatedAt(kpi.getCalculatedAt()).build();
    }

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
}