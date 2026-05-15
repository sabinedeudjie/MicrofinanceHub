package org.example.loanservice.controller;

import org.example.loanservice.dto.equest.LoanApplicationRequest;
import org.example.loanservice.dto.response.AmortizationResponse;
import org.example.loanservice.dto.response.EligibilityResponse;
import org.example.loanservice.dto.response.LoanApplicationResponse;
import org.example.loanservice.dto.response.LoanResponse;
import org.example.loanservice.dto.response.LoanStatsResponse;
import org.example.loanservice.dto.response.PortfolioStatsResponse;
import org.example.loanservice.model.AmortizationSchedule;
import org.example.loanservice.model.Loan;
import org.example.loanservice.model.LoanApplication;
import org.example.loanservice.model.enums.ApplicationStatus;
import org.example.loanservice.model.enums.LoanStatus;
import org.example.loanservice.repository.LoanApplicationRepository;
import org.example.loanservice.repository.LoanRepository;
import org.example.loanservice.service.EligibilityService;
import org.example.loanservice.service.LoanService;
import org.example.loanservice.service.ReportingStatsService;
import org.example.loanservice.service.ScheduleManagementService;

import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {
    
    private final LoanService loanService;
    private final EligibilityService eligibilityService;
    private final ScheduleManagementService scheduleManagementService;
    private final ReportingStatsService reportingStatsService;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanRepository loanRepository;
    
    // 
    //  PUBLICS OU PROTÉGÉS
    // 
    
    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<LoanApplicationResponse> applyForLoan(
            @Valid @RequestBody LoanApplicationRequest request,
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-User-Id") String userId) {
        
        LoanApplicationResponse response = loanService.applyForLoan(request, userId, token);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/eligibility/{clientId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<EligibilityResponse> checkEligibility(
            @PathVariable String clientId,
            @RequestParam BigDecimal amount,
            @RequestParam Integer termMonths,
            @RequestParam(required = false) String accountNumber,
            @RequestHeader("Authorization") String token) {
        
        EligibilityResponse response;
        
        if (accountNumber != null && !accountNumber.isEmpty()) {
            response = eligibilityService.checkEligibility(clientId, accountNumber, amount, termMonths, token);
        } else {
            response = eligibilityService.checkEligibility(clientId, amount, termMonths, token);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<List<LoanResponse>> getClientLoans(@PathVariable String clientId) {
        return ResponseEntity.ok(loanService.getClientLoans(clientId));
    }
    
    @GetMapping("/{loanId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<LoanResponse> getLoan(@PathVariable String loanId) {
        return ResponseEntity.ok(loanService.getLoan(loanId));
    }
    
    @GetMapping("/{loanId}/amortization")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<AmortizationResponse> getAmortizationSchedule(@PathVariable String loanId) {
        return ResponseEntity.ok(loanService.getAmortizationSchedule(loanId));
    }

    @GetMapping("/applications/pending")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<Page<LoanApplicationResponse>> getPendingApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<LoanApplicationResponse> pendingApplications = loanService.getPendingApplications(pageable);
        return ResponseEntity.ok(pendingApplications);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<Page<LoanResponse>> getAllLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page, size);
        Page<LoanResponse> loans;
        if (status != null && !status.isBlank()) {
            LoanStatus loanStatus = LoanStatus.valueOf(status.toUpperCase());
            loans = loanRepository.findByStatus(loanStatus, pageable).map(loanService::mapToLoanResponse);
        } else {
            loans = loanRepository.findAll(pageable).map(loanService::mapToLoanResponse);
        }
        return ResponseEntity.ok(loans);
    }

    @PostMapping("/by-clients")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_AGENCE', 'AGENT')")
    public ResponseEntity<Page<LoanResponse>> getLoansByClients(
            @RequestBody List<String> clientIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String status) {

        if (clientIds == null || clientIds.isEmpty()) return ResponseEntity.ok(Page.empty());
        Pageable pageable = PageRequest.of(page, size);
        Page<Loan> loans;
        if (status != null && !status.isBlank()) {
            LoanStatus loanStatus = LoanStatus.valueOf(status.toUpperCase());
            loans = loanRepository.findByClientIdInAndStatus(clientIds, loanStatus, pageable);
        } else {
            loans = loanRepository.findByClientIdIn(clientIds, pageable);
        }
        return ResponseEntity.ok(loans.map(loanService::mapToLoanResponse));
    }

    @PostMapping("/applications/pending/by-clients")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_AGENCE', 'AGENT')")
    public ResponseEntity<Page<LoanApplicationResponse>> getPendingApplicationsByClients(
            @RequestBody List<String> clientIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        if (clientIds == null || clientIds.isEmpty()) return ResponseEntity.ok(Page.empty());
        Pageable pageable = PageRequest.of(page, size);
        Page<LoanApplication> apps = loanApplicationRepository.findByClientIdInAndStatus(
                clientIds, ApplicationStatus.PENDING, pageable);
        return ResponseEntity.ok(apps.map(loanService::mapToApplicationResponse));
    }

    @PostMapping("/applications/by-clients")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_AGENCE', 'AGENT')")
    public ResponseEntity<List<LoanApplicationResponse>> getApplicationsByClients(
            @RequestBody List<String> clientIds) {

        if (clientIds == null || clientIds.isEmpty()) return ResponseEntity.ok(List.of());
        List<LoanApplicationResponse> apps = loanService.getApplicationsByClientIds(clientIds);
        return ResponseEntity.ok(apps);
    }

    @GetMapping("/{loanId}/status")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<String> getLoanStatus(@PathVariable String loanId) {
       Loan loan = loanService.getLoanById(loanId);
       return ResponseEntity.ok(loan.getStatus().name());
    }
    
    @GetMapping("/applications/{applicationId}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'CLIENT', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<LoanApplicationResponse> getApplication(@PathVariable String applicationId) {
        LoanApplicationResponse application = loanService.getApplication(applicationId);
        return ResponseEntity.ok(application);
    }

    @GetMapping("/applications/client/{clientId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<List<LoanApplicationResponse>> getClientApplications(@PathVariable String clientId) {
        return ResponseEntity.ok(loanService.getClientApplications(clientId));
    }

    @GetMapping("/{loanId}/schedules")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<List<AmortizationSchedule>> getLoanSchedules(@PathVariable String loanId) {
         return ResponseEntity.ok(scheduleManagementService.getSchedulesByLoanId(loanId));
    }

    /* Endpoint pour récupérer l'ID du client associé à un prêt
     * Utilisé par Repayment Service
     */
    @GetMapping("/{loanId}/client-id")
    public ResponseEntity<String> getClientIdByLoanId(@PathVariable String loanId) {
       log.info("du client ID pour loanId: {}", loanId);
       Loan loan = loanService.getLoanById(loanId);
       if (loan == null) {
          return ResponseEntity.notFound().build();
       }
       return ResponseEntity.ok(loan.getClientId());
    }
    
    /**
    * Marque une échéance comme payée
    */
    @PutMapping("/{loanId}/schedules/{installmentNumber}/pay")
    public ResponseEntity<Void> markScheduleAsPaid(
        @PathVariable String loanId,
        @PathVariable Integer installmentNumber,
        @RequestParam String paymentId,
        @RequestHeader("Authorization") String authorization) {
    
       log.info("de l'échéance comme payée: loanId={}, installment={}, paymentId={}", 
          loanId, installmentNumber, paymentId);
    
       scheduleManagementService.markScheduleAsPaid(loanId, installmentNumber, paymentId);
       return ResponseEntity.ok().build();
    }

    // 
    //  POUR STATISTIQUES (ADMIN et AGENT)
    // 

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<LoanStatsResponse> getLoanStats(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate",   required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        if (startDate == null) startDate = LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);
        if (endDate   == null) endDate   = LocalDateTime.now();
        return ResponseEntity.ok(reportingStatsService.getLoanStats(startDate, endDate));
    }
    
    @GetMapping("/portfolio/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<PortfolioStatsResponse> getPortfolioStats() {
        return ResponseEntity.ok(reportingStatsService.getPortfolioStats());
    }
     
    @PostMapping("/stats/by-clients")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<LoanStatsResponse> getLoanStatsForClients(
            @RequestBody List<String> clientIds,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate",   required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        if (startDate == null) startDate = LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);
        if (endDate   == null) endDate   = LocalDateTime.now();
        
        log.info("pour {} clients du {} au {}", clientIds.size(), startDate, endDate);
        LoanStatsResponse stats = reportingStatsService.getLoanStatsForClients(clientIds, startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/stats/by-agent")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<LoanStatsResponse> getLoanStatsForAgent(
            @RequestParam String agentId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate",   required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestHeader("X-User-Id") String authenticatedAgentId) {
        
        if (!agentId.equals(authenticatedAgentId)) {
            log.warn("{} tente d'accéder aux données de {}", authenticatedAgentId, agentId);
            return ResponseEntity.status(403).build();
        }
        
        if (startDate == null) startDate = LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);
        if (endDate   == null) endDate   = LocalDateTime.now();
        
        log.info("Statistiques de prêts pour l'agent: {} du {} au {}", agentId, startDate, endDate);
        
        List<String> clientIds = getClientIdsByAgent(agentId);
        
        if (clientIds.isEmpty()) {
            return ResponseEntity.ok(createEmptyLoanStats());
        }
        
        LoanStatsResponse stats = reportingStatsService.getLoanStatsForClients(clientIds, startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/applications/pending/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<Long> countPendingApplicationsForClients(@RequestBody List<String> clientIds) {
        long count = loanApplicationRepository.countByClientIdInAndStatus(clientIds, ApplicationStatus.PENDING);
        return ResponseEntity.ok(count);
    }

    // 
    //  POUR AGENT (spécifiques)
    // 

    @PostMapping("/by-agent")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<List<LoanResponse>> getLoansByAgent(
            @RequestParam String agentId,
            @RequestHeader("X-User-Id") String authenticatedAgentId) {
        
        if (!agentId.equals(authenticatedAgentId)) {
            return ResponseEntity.status(403).build();
        }
        
        log.info("des prêts pour l'agent: {}", agentId);
        
        List<String> clientIds = getClientIdsByAgent(agentId);
        
        if (clientIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        
        List<LoanResponse> loans = loanService.getLoansByClientIds(clientIds);
        return ResponseEntity.ok(loans);
    }

    @PostMapping("/portfolio/stats/by-agent")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<PortfolioStatsResponse> getPortfolioStatsForAgent(
            @RequestParam String agentId,
            @RequestHeader("X-User-Id") String authenticatedAgentId) {
        
        if (!agentId.equals(authenticatedAgentId)) {
            return ResponseEntity.status(403).build();
        }
        
        log.info("des statistiques portefeuille pour l'agent: {}", agentId);
        
        List<String> clientIds = getClientIdsByAgent(agentId);
        
        if (clientIds.isEmpty()) {
            return ResponseEntity.ok(createEmptyPortfolioStats());
        }
        
        PortfolioStatsResponse stats = reportingStatsService.getPortfolioStatsForClients(clientIds);
        return ResponseEntity.ok(stats);
    }

    // 
    //  UTILITAIRES
    // 

    private List<String> getClientIdsByAgent(String agentId) {
        //  les IDs des clients via les demandes de prêt examinées par l'agent
        return loanApplicationRepository.findDistinctClientIdsByReviewedBy(agentId);
    }

    private LoanStatsResponse createEmptyLoanStats() {
        return LoanStatsResponse.builder()
            .totalApplications(0L)
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

    private PortfolioStatsResponse createEmptyPortfolioStats() {
        return PortfolioStatsResponse.builder()
            .totalOutstanding(BigDecimal.ZERO)
            .atRisk30Days(BigDecimal.ZERO)
            .atRisk90Days(BigDecimal.ZERO)
            .recoveryRate(0.0)
            .build();
    }

    //  pour les autres services

/**
 * Endpoint interne pour vérifier si un client a des prêts actifs
 */
@GetMapping("/internal/client/{clientId}/has-active-loans")
public ResponseEntity<Boolean> hasActiveLoans(@PathVariable String clientId) {
    long activeLoans = loanRepository.countByClientIdAndStatus(clientId, LoanStatus.ACTIVE);
    return ResponseEntity.ok(activeLoans > 0);
}

/**
 * Endpoint interne pour obtenir le statut des prêts d'un client
 */
@GetMapping("/internal/client/{clientId}/loan-status")
public ResponseEntity<String> getClientLoanStatus(@PathVariable String clientId) {
    List<Loan> loans = loanRepository.findByClientId(clientId);
    
    if (loans.isEmpty()) {
        return ResponseEntity.ok("NO_LOANS");
    }
    
    boolean hasActive = loans.stream().anyMatch(l -> l.getStatus() == LoanStatus.ACTIVE);
    boolean hasPending = loans.stream().anyMatch(l -> l.getStatus() == LoanStatus.PENDING_APPROVAL);
    boolean hasDefaulted = loans.stream().anyMatch(l -> l.getStatus() == LoanStatus.DEFAULTED);
    
    if (hasActive) return ResponseEntity.ok("ACTIVE_LOAN");
    if (hasPending) return ResponseEntity.ok("PENDING_APPROVAL");
    if (hasDefaulted) return ResponseEntity.ok("DEFAULTED");
    
    return ResponseEntity.ok("COMPLETED");
}
    /**
     *  Statistiques du portefeuille pour une liste de clients
     * Utilisé par Reporting Service
     */
    @PostMapping("/portfolio/stats/by-clients")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<PortfolioStatsResponse> getPortfolioStatsForClients(
            @RequestBody List<String> clientIds,
            @RequestHeader("Authorization") String token) {
        
        log.info("du portefeuille pour {} clients", clientIds != null ? clientIds.size() : 0);
        
        if (clientIds == null || clientIds.isEmpty()) {
            return ResponseEntity.ok(createEmptyPortfolioStats());
        }
        
        PortfolioStatsResponse stats = reportingStatsService.getPortfolioStatsForClients(clientIds);
        return ResponseEntity.ok(stats);
    }

    // 
    //  APPELÉS PAR repayment-service
    // 

    @GetMapping("/{loanId}/exists")
    public ResponseEntity<Boolean> loanExists(@PathVariable String loanId) {
        boolean exists = loanRepository.existsById(loanId);
        return ResponseEntity.ok(exists);
    }
}