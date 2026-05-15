package org.example.loanservice.service;

import org.example.loanservice.dto.response.LoanStatsResponse;
import org.example.loanservice.dto.response.PortfolioStatsResponse;
import org.example.loanservice.model.Loan;
import org.example.loanservice.model.LoanApplication;
import org.example.loanservice.model.enums.ApplicationStatus;
import org.example.loanservice.model.enums.LoanStatus;
import org.example.loanservice.repository.LoanApplicationRepository;
import org.example.loanservice.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingStatsService {
    
    private final LoanRepository loanRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    
    //  unifié avec Object
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    
    private static final long CACHE_DURATION_SECONDS = 60; //  valide 60 secondes
    
    private record CacheEntry(Object value, long timestamp) {}
    
    // 
    //  GLOBALES
    // 
    
    public LoanStatsResponse getLoanStats(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) startDate = LocalDateTime.of(2000, 1, 1, 0, 0);
        if (endDate == null) endDate = LocalDateTime.now();
        
        String cacheKey = "loanStats_" + startDate.toString() + "_" + endDate.toString();
        
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && 
            (System.currentTimeMillis() - cached.timestamp()) < TimeUnit.SECONDS.toMillis(CACHE_DURATION_SECONDS)) {
            log.debug("des statistiques depuis le cache");
            return (LoanStatsResponse) cached.value();
        }
        
        lock.lock();
        try {
            cached = cache.get(cacheKey);
            if (cached != null && 
                (System.currentTimeMillis() - cached.timestamp()) < TimeUnit.SECONDS.toMillis(CACHE_DURATION_SECONDS)) {
                return (LoanStatsResponse) cached.value();
            }
            
            log.info("des statistiques de prêts du {} au {}", startDate, endDate);
            
            Long totalApplications = loanApplicationRepository.countByApplicationDateBetween(startDate, endDate);
            Long approvedApplications = loanApplicationRepository.countByStatusAndApplicationDateBetween(
                ApplicationStatus.APPROVED, startDate, endDate);
            Long rejectedApplications = loanApplicationRepository.countByStatusAndApplicationDateBetween(
                ApplicationStatus.REJECTED, startDate, endDate);
            Long pendingCount = loanApplicationRepository.countByStatusAndApplicationDateBetween(ApplicationStatus.PENDING, startDate, endDate);
            Long underReviewCount = loanApplicationRepository.countByStatusAndApplicationDateBetween(ApplicationStatus.UNDER_REVIEW, startDate, endDate);
            Long pendingApplications = (pendingCount != null ? pendingCount : 0L) + (underReviewCount != null ? underReviewCount : 0L);
            
            Long disbursedLoans = loanRepository.countByDisbursementDateBetweenAndStatusIn(
                startDate, endDate, List.of(LoanStatus.ACTIVE, LoanStatus.COMPLETED));
            Long activeLoans = loanRepository.countByStatus(LoanStatus.ACTIVE);
            Long completedLoans = loanRepository.countByDisbursementDateBetweenAndStatus(startDate, endDate, LoanStatus.COMPLETED);
            Long defaultedLoans = loanRepository.countByDisbursementDateBetweenAndStatus(startDate, endDate, LoanStatus.DEFAULTED);
            
            BigDecimal totalDisbursedAmount = loanRepository.sumAmountByDisbursementDateBetween(startDate, endDate);
            BigDecimal totalRepaidAmount = loanRepository.sumRepaidAmountByDisbursementDateBetween(startDate, endDate);
            BigDecimal outstandingAmount = loanRepository.sumRemainingBalanceByStatus(LoanStatus.ACTIVE);
            
            double approvalRate = (totalApplications != null && totalApplications > 0) ? 
                    (double) (approvedApplications != null ? approvedApplications : 0) / totalApplications * 100 : 0.0;
            double defaultRate = (totalApplications != null && totalApplications > 0) ? 
                    (double) (defaultedLoans != null ? defaultedLoans : 0) / totalApplications * 100 : 0.0;
            double recoveryRate = (totalDisbursedAmount != null && totalDisbursedAmount.compareTo(BigDecimal.ZERO) > 0) ?
                    (totalRepaidAmount != null ? totalRepaidAmount : BigDecimal.ZERO)
                        .divide(totalDisbursedAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;
            
            LoanStatsResponse response = LoanStatsResponse.builder()
                    .totalApplications(totalApplications != null ? totalApplications : 0L)
                    .pendingApplications(pendingApplications)
                    .approvedApplications(approvedApplications != null ? approvedApplications : 0L)
                    .rejectedApplications(rejectedApplications != null ? rejectedApplications : 0L)
                    .disbursedLoans(disbursedLoans != null ? disbursedLoans : 0L)
                    .activeLoans(activeLoans != null ? activeLoans : 0L)
                    .completedLoans(completedLoans != null ? completedLoans : 0L)
                    .defaultedLoans(defaultedLoans != null ? defaultedLoans : 0L)
                    .totalDisbursedAmount(totalDisbursedAmount != null ? totalDisbursedAmount : BigDecimal.ZERO)
                    .totalRepaidAmount(totalRepaidAmount != null ? totalRepaidAmount : BigDecimal.ZERO)
                    .outstandingAmount(outstandingAmount != null ? outstandingAmount : BigDecimal.ZERO)
                    .approvalRate(approvalRate)
                    .defaultRate(defaultRate)
                    .recoveryRate(recoveryRate)
                    .build();
            
            cache.put(cacheKey, new CacheEntry(response, System.currentTimeMillis()));
            return response;
            
        } finally {
            lock.unlock();
        }
    }
    
    public PortfolioStatsResponse getPortfolioStats() {
        String cacheKey = "portfolioStats";
        
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && 
            (System.currentTimeMillis() - cached.timestamp()) < TimeUnit.SECONDS.toMillis(CACHE_DURATION_SECONDS)) {
            log.debug("Retour des statistiques du portefeuille depuis le cache");
            return (PortfolioStatsResponse) cached.value();
        }
        
        lock.lock();
        try {
            cached = cache.get(cacheKey);
            if (cached != null && 
                (System.currentTimeMillis() - cached.timestamp()) < TimeUnit.SECONDS.toMillis(CACHE_DURATION_SECONDS)) {
                return (PortfolioStatsResponse) cached.value();
            }
            
            log.info("des statistiques du portefeuille");
            
            BigDecimal totalOutstanding = loanRepository.sumRemainingBalanceByStatus(LoanStatus.ACTIVE);
            
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
            
            BigDecimal atRisk30Days = loanRepository.sumMonthlyPaymentByNextPaymentDateBefore(thirtyDaysAgo);
            BigDecimal atRisk90Days = loanRepository.sumMonthlyPaymentByNextPaymentDateBefore(ninetyDaysAgo);
            
            BigDecimal totalDisbursed = loanRepository.sumAmountByStatus(LoanStatus.ACTIVE);
            BigDecimal totalRepaid = loanRepository.sumRepaidAmountByStatus(LoanStatus.ACTIVE);
            
            double recoveryRate = (totalDisbursed != null && totalDisbursed.compareTo(BigDecimal.ZERO) > 0) ?
                    (totalRepaid != null ? totalRepaid : BigDecimal.ZERO)
                        .divide(totalDisbursed, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;
            
            PortfolioStatsResponse response = PortfolioStatsResponse.builder()
                    .totalOutstanding(totalOutstanding != null ? totalOutstanding : BigDecimal.ZERO)
                    .atRisk30Days(atRisk30Days != null ? atRisk30Days : BigDecimal.ZERO)
                    .atRisk90Days(atRisk90Days != null ? atRisk90Days : BigDecimal.ZERO)
                    .recoveryRate(recoveryRate)
                    .build();
            
            cache.put(cacheKey, new CacheEntry(response, System.currentTimeMillis()));
            return response;
            
        } finally {
            lock.unlock();
        }
    }
    
    // 
    //  POUR LISTE DE CLIENTS
    // 
    
    public LoanStatsResponse getLoanStatsForClients(List<String> clientIds, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) startDate = LocalDateTime.of(2000, 1, 1, 0, 0);
        if (endDate == null) endDate = LocalDateTime.now();
        
        log.info("des statistiques de prêts pour {} clients du {} au {}", 
            clientIds != null ? clientIds.size() : 0, startDate, endDate);
        
        if (clientIds == null || clientIds.isEmpty()) {
            log.warn("de clients vide, retour de statistiques vides");
            return createEmptyLoanStats();
        }
        
        String cacheKey = "loanStats_clients_" + String.join("_", clientIds) + "_" + startDate.toString() + "_" + endDate.toString();
        
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && 
            (System.currentTimeMillis() - cached.timestamp()) < TimeUnit.SECONDS.toMillis(CACHE_DURATION_SECONDS)) {
            log.debug("des statistiques depuis le cache");
            return (LoanStatsResponse) cached.value();
        }
        
        lock.lock();
        try {
            cached = cache.get(cacheKey);
            if (cached != null && 
                (System.currentTimeMillis() - cached.timestamp()) < TimeUnit.SECONDS.toMillis(CACHE_DURATION_SECONDS)) {
                return (LoanStatsResponse) cached.value();
            }
            
            List<LoanApplication> applications = loanApplicationRepository.findByClientIdInAndApplicationDateBetween(clientIds, startDate, endDate);
            List<Loan> loans = loanRepository.findByClientIdInAndDisbursementDateBetween(clientIds, startDate, endDate);
            
            long totalApplications = applications.size();
            long pendingApplications = applications.stream()
                    .filter(a -> a.getStatus() == ApplicationStatus.PENDING || a.getStatus() == ApplicationStatus.UNDER_REVIEW)
                    .count();
            long approvedApplications = applications.stream()
                    .filter(a -> a.getStatus() == ApplicationStatus.APPROVED)
                    .count();
            long rejectedApplications = applications.stream()
                    .filter(a -> a.getStatus() == ApplicationStatus.REJECTED)
                    .count();
            
            long disbursedLoans = loans.stream()
                    .filter(l -> l.getStatus() == LoanStatus.ACTIVE || l.getStatus() == LoanStatus.COMPLETED)
                    .count();
            long activeLoansCount = loanRepository.countByClientIdInAndStatus(clientIds, LoanStatus.ACTIVE);
            long completedLoans = loans.stream()
                    .filter(l -> l.getStatus() == LoanStatus.COMPLETED)
                    .count();
            long defaultedLoans = loans.stream()
                    .filter(l -> l.getStatus() == LoanStatus.DEFAULTED)
                    .count();
            
            BigDecimal totalDisbursedAmount = loans.stream()
                    .map(Loan::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalRepaidAmount = loans.stream()
                    .map(l -> {
                        if (l.getTotalRepayment() != null && l.getRemainingBalance() != null) {
                            return l.getTotalRepayment().subtract(l.getRemainingBalance());
                        }
                        return BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal outstandingAmount = loanRepository.sumRemainingBalanceByClientIdInAndStatus(clientIds, LoanStatus.ACTIVE);
            
            double approvalRate = totalApplications > 0 ? (double) approvedApplications / totalApplications * 100 : 0.0;
            double defaultRate = totalApplications > 0 ? (double) defaultedLoans / totalApplications * 100 : 0.0;
            double recoveryRate = totalDisbursedAmount.compareTo(BigDecimal.ZERO) > 0 ?
                    totalRepaidAmount.divide(totalDisbursedAmount, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;
            
            LoanStatsResponse response = LoanStatsResponse.builder()
                    .totalApplications(totalApplications)
                    .pendingApplications(pendingApplications)
                    .approvedApplications(approvedApplications)
                    .rejectedApplications(rejectedApplications)
                    .disbursedLoans(disbursedLoans)
                    .activeLoans(activeLoansCount)
                    .completedLoans(completedLoans)
                    .defaultedLoans(defaultedLoans)
                    .totalDisbursedAmount(totalDisbursedAmount)
                    .totalRepaidAmount(totalRepaidAmount)
                    .outstandingAmount(outstandingAmount)
                    .approvalRate(approvalRate)
                    .defaultRate(defaultRate)
                    .recoveryRate(recoveryRate)
                    .build();
            
            cache.put(cacheKey, new CacheEntry(response, System.currentTimeMillis()));
            return response;
            
        } finally {
            lock.unlock();
        }
    }
    
    public PortfolioStatsResponse getPortfolioStatsForClients(List<String> clientIds) {
        log.info("des statistiques du portefeuille pour {} clients",
            clientIds != null ? clientIds.size() : 0);
        
        if (clientIds == null || clientIds.isEmpty()) {
            log.warn("de clients vide, retour de statistiques vides");
            return createEmptyPortfolioStats();
        }
        
        String cacheKey = "portfolioStats_clients_" + String.join("_", clientIds);
        
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && 
            (System.currentTimeMillis() - cached.timestamp()) < TimeUnit.SECONDS.toMillis(CACHE_DURATION_SECONDS)) {
            log.debug("Retour des statistiques du portefeuille depuis le cache");
            return (PortfolioStatsResponse) cached.value();
        }
        
        lock.lock();
        try {
            cached = cache.get(cacheKey);
            if (cached != null && 
                (System.currentTimeMillis() - cached.timestamp()) < TimeUnit.SECONDS.toMillis(CACHE_DURATION_SECONDS)) {
                return (PortfolioStatsResponse) cached.value();
            }
            
            log.info("des statistiques du portefeuille pour {} clients", clientIds.size());
            
            List<Loan> activeLoans = loanRepository.findByClientIdInAndStatus(clientIds, LoanStatus.ACTIVE);
            
            BigDecimal totalOutstanding = activeLoans.stream()
                    .map(Loan::getRemainingBalance)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
            
            BigDecimal atRisk30Days = activeLoans.stream()
                    .filter(l -> l.getNextPaymentDate() != null && l.getNextPaymentDate().isBefore(thirtyDaysAgo))
                    .map(Loan::getMonthlyPayment)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal atRisk90Days = activeLoans.stream()
                    .filter(l -> l.getNextPaymentDate() != null && l.getNextPaymentDate().isBefore(ninetyDaysAgo))
                    .map(Loan::getMonthlyPayment)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalDisbursed = activeLoans.stream()
                    .map(Loan::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalRepaid = activeLoans.stream()
                    .map(l -> {
                        if (l.getTotalRepayment() != null && l.getRemainingBalance() != null) {
                            return l.getTotalRepayment().subtract(l.getRemainingBalance());
                        }
                        return BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            double recoveryRate = totalDisbursed.compareTo(BigDecimal.ZERO) > 0 ?
                    totalRepaid.divide(totalDisbursed, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;
            
            PortfolioStatsResponse response = PortfolioStatsResponse.builder()
                    .totalOutstanding(totalOutstanding != null ? totalOutstanding : BigDecimal.ZERO)
                    .atRisk30Days(atRisk30Days != null ? atRisk30Days : BigDecimal.ZERO)
                    .atRisk90Days(atRisk90Days != null ? atRisk90Days : BigDecimal.ZERO)
                    .recoveryRate(recoveryRate)
                    .build();
            
            cache.put(cacheKey, new CacheEntry(response, System.currentTimeMillis()));
            return response;
            
        } finally {
            lock.unlock();
        }
    }
    
    // 
    //  UTILITAIRES
    // 
    
    /**
     * Vide le cache (peut être appelé après un événement qui modifie les données)
     */
    public void clearCache() {
        cache.clear();
        log.info("des statistiques vidé");
    }
    
    /**
     * Crée des statistiques de prêts vides
     */
    private LoanStatsResponse createEmptyLoanStats() {
        return LoanStatsResponse.builder()
                .totalApplications(0L)
                .pendingApplications(0L)
                .approvedApplications(0L)
                .rejectedApplications(0L)
                .disbursedLoans(0L)
                .activeLoans(0L)
                .completedLoans(0L)
                .defaultedLoans(0L)
                .totalDisbursedAmount(BigDecimal.ZERO)
                .totalRepaidAmount(BigDecimal.ZERO)
                .outstandingAmount(BigDecimal.ZERO)
                .approvalRate(0.0)
                .defaultRate(0.0)
                .recoveryRate(0.0)
                .build();
    }
    
    /**
     * Crée des statistiques de portefeuille vides
     */
    private PortfolioStatsResponse createEmptyPortfolioStats() {
        return PortfolioStatsResponse.builder()
                .totalOutstanding(BigDecimal.ZERO)
                .atRisk30Days(BigDecimal.ZERO)
                .atRisk90Days(BigDecimal.ZERO)
                .recoveryRate(0.0)
                .build();
    }

    public List<String> getClientIdsByAgent(String agentId) {
        log.info("des IDs clients pour l'agent: {}", agentId);
        try {
            return loanApplicationRepository.findDistinctClientIdsByReviewedBy(agentId);
        } catch (Exception e) {
           log.error("lors de la récupération des clients pour l'agent {}: {}", agentId, e.getMessage());
           return List.of();
        }
    }

    /**
 * Récupère les IDs des clients pour lesquels un agent a examiné des demandes
 */
public List<String> getClientIdsByReviewedBy(String reviewedBy) {
    log.info("des IDs clients pour l'agent: {}", reviewedBy);
    
    if (reviewedBy == null || reviewedBy.isEmpty()) {
        return List.of();
    }
    
    try {
        return loanApplicationRepository.findDistinctClientIdsByReviewedBy(reviewedBy);
    } catch (Exception e) {
        log.error("lors de la récupération des clients pour l'agent {}: {}", reviewedBy, e.getMessage());
        return List.of();
    }
}

/**
 * Récupère les statistiques de prêts pour les clients d'un agent
 */
public LoanStatsResponse getLoanStatsForAgent(String reviewedBy, LocalDateTime startDate, LocalDateTime endDate) {
    log.info("des statistiques de prêts pour l'agent: {}", reviewedBy);
    
    if (reviewedBy == null || reviewedBy.isEmpty()) {
        return createEmptyLoanStats();
    }
    
    //  les IDs des clients que l'agent a examinés
    List<String> clientIds = getClientIdsByReviewedBy(reviewedBy);
    
    if (clientIds.isEmpty()) {
        log.warn("client trouvé pour l'agent: {}", reviewedBy);
        return createEmptyLoanStats();
    }
    
    return getLoanStatsForClients(clientIds, startDate, endDate);
}

/**
 * Récupère les statistiques du portefeuille pour les clients d'un agent
 */
public PortfolioStatsResponse getPortfolioStatsForAgent(String reviewedBy) {
    log.info("des statistiques du portefeuille pour l'agent: {}", reviewedBy);
    
    if (reviewedBy == null || reviewedBy.isEmpty()) {
        return createEmptyPortfolioStats();
    }
    
    //  les IDs des clients que l'agent a examinés
    List<String> clientIds = getClientIdsByReviewedBy(reviewedBy);
    
    if (clientIds.isEmpty()) {
        log.warn("client trouvé pour l'agent: {}", reviewedBy);
        return createEmptyPortfolioStats();
    }
    
    return getPortfolioStatsForClients(clientIds);
}

}