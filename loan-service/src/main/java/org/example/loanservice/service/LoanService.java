package org.example.loanservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.example.loanservice.client.AccountServiceClient;
import org.example.loanservice.client.AgencyServiceClient;
import org.example.loanservice.client.ClientServiceClient;
import org.example.loanservice.client.model.AccountInfo;
import org.example.loanservice.client.model.ClientInfo;
import org.example.loanservice.dto.equest.LoanApplicationRequest;
import org.example.loanservice.dto.equest.LoanApprovalRequest;
import org.example.loanservice.dto.response.AgencyResponse;
import org.example.loanservice.dto.response.AgentAssignmentResponse;
import org.example.loanservice.dto.response.AmortizationResponse;
import org.example.loanservice.dto.response.EligibilityResponse;
import org.example.loanservice.dto.response.LoanApplicationResponse;
import org.example.loanservice.dto.response.LoanResponse;
import org.example.loanservice.event.LoanEventPublisher;
import org.example.loanservice.exception.IneligibleClientException;
import org.example.loanservice.exception.LoanAlreadyProcessedException;
import org.example.loanservice.exception.LoanApplicationNotFoundException;
import org.example.loanservice.model.Loan;
import org.example.loanservice.model.LoanApplication;
import org.example.loanservice.model.LoanProduct;
import org.example.loanservice.model.enums.ApplicationStatus;
import org.example.loanservice.model.enums.LoanStatus;
import org.example.loanservice.model.enums.RepaymentFrequency;
import org.example.loanservice.repository.LoanApplicationRepository;
import org.example.loanservice.repository.LoanRepository;

import feign.FeignException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {
    
    private final LoanRepository loanRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final AmortizationService amortizationService;
    private final EligibilityService eligibilityService;
    private final LoanEventPublisher eventPublisher;
    private final ClientServiceClient clientServiceClient;
    private final ScheduleManagementService scheduleManagementService;
    private final AccountServiceClient accountServiceClient;
    private final AgencyServiceClient agencyServiceClient;
    private final LoanProductService  loanProductService;
    
    @Transactional
    public LoanApplicationResponse applyForLoan(LoanApplicationRequest request, String userId, String token) {
        log.info("Nouvelle demande de pret pour le client: {}", request.getClientId());
        
        ClientInfo clientInfo = null;
        try {
            clientInfo = clientServiceClient.getClientInfo(request.getClientId(), token);
            if (clientInfo == null) {
                log.error("null pour clientId: {}", request.getClientId());
                throw new RuntimeException("Client non trouvé");
            }
            log.info("client récupérées: {} {}, email: {}", 
                clientInfo.getFirstName(), 
                clientInfo.getLastName(), 
                clientInfo.getEmail());
        } catch (Exception e) {
            log.error("de récupérer les infos du client: {}", e.getMessage());
            throw new RuntimeException("Client non trouvé: " + e.getMessage());
        }

        EligibilityResponse eligibility = eligibilityService.checkEligibility(
            request.getClientId(),
            request.getAccountNumber(),
            request.getRequestedAmount(),
            request.getTermMonths(),
            token
        );
        
        if (!eligibility.isEligible()) {
            throw new IneligibleClientException(eligibility.getMessage());
        }

        long activeLoans = loanRepository.countByClientIdAndStatus(
            request.getClientId(), 
            LoanStatus.ACTIVE
        );
        
        if (activeLoans >= 3) {
            throw new IneligibleClientException("Maximum 3 prêts actifs simultanément");
        }

        LoanApplication application = LoanApplication.builder()
            .clientId(request.getClientId())
            .accountNumber(request.getAccountNumber()) 
            .clientEmail(clientInfo.getEmail())           
            .clientFirstName(clientInfo.getFirstName())  
            .clientLastName(clientInfo.getLastName())    
            .requestedAmount(request.getRequestedAmount())
            .termMonths(request.getTermMonths())
            .purpose(request.getPurpose())
            .monthlyIncome(request.getMonthlyIncome())
            .employmentStatus(request.getEmploymentStatus())
            .status(ApplicationStatus.PENDING)
            .build();
        
        loanApplicationRepository.save(application);
        
        //  événement
        eventPublisher.publishLoanApplied(application);
        
        return mapToApplicationResponse(application);
    }
    
   @Transactional
public LoanResponse approveLoan(String applicationId, String approvedBy, String token) {
    log.info("║                    APPROBATION DE PRÊT                         ║");
    log.info("║ Application ID: {}", applicationId);
    log.info("║ Approuvé par   : {}", approvedBy);

    log.info("/7] Récupération de la demande...");
    LoanApplication application = loanApplicationRepository.findById(applicationId)
        .orElseThrow(() -> {
            log.error("non trouvée: {}", applicationId);
            return new RuntimeException("Demande non trouvée");
        });
    log.info("trouvée pour le client: {}", application.getClientId());
    
    log.info("/7] Vérification du statut...");
    if (application.getStatus() != ApplicationStatus.PENDING) {
        log.error("déjà traitée - Statut actuel: {}", application.getStatus());
        throw new LoanAlreadyProcessedException(applicationId, application.getStatus().name());
    }
    log.info("PENDING - Traitement possible");
    
    log.info("/7] Vérification de l'éligibilité...");
    EligibilityResponse eligibility = eligibilityService.checkEligibility(
        application.getClientId(),
        application.getRequestedAmount(),
        application.getTermMonths(),
        token
    ); 

    log.info(".5/7] Vérification du compte...");
    try {
        Boolean accountExists = accountServiceClient.accountExists(
            application.getClientId(), token);
        if (accountExists == null || !accountExists) {
            log.warn("non trouvé pour le client: {}", application.getClientId());
            application.setStatus(ApplicationStatus.REJECTED);
            application.setRejectionReason("Compte bancaire requis. Veuillez d'abord ouvrir un compte.");
            application.setReviewedDate(LocalDateTime.now());
            application.setReviewedBy(approvedBy);
            loanApplicationRepository.save(application);
            throw new RuntimeException("Compte bancaire requis pour obtenir un prêt");
        }
        log.info("validé pour le client");
    } catch (FeignException e) {
        log.error("lors de la vérification du compte: {}", e.getMessage());
    }
    
    if (!eligibility.isEligible()) {
        log.warn("non éligible: {}", eligibility.getMessage());
        application.setStatus(ApplicationStatus.REJECTED);
        application.setRejectionReason("Client non éligible après vérification: " + eligibility.getMessage());
        application.setReviewedDate(LocalDateTime.now());
        application.setReviewedBy(approvedBy);
        loanApplicationRepository.save(application);
        throw new RuntimeException("Client non éligible: " + eligibility.getMessage());
    }
    log.info("éligible");
    
    log.info("/7] Calcul des mensualités...");
    BigDecimal interestRate = new BigDecimal("0.15"); // % par an
    BigDecimal monthlyInterestRate = interestRate.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
    BigDecimal monthlyPayment = calculateMonthlyPayment(
        application.getRequestedAmount(),
        monthlyInterestRate,
        application.getTermMonths()
    );
    BigDecimal totalRepayment = monthlyPayment.multiply(BigDecimal.valueOf(application.getTermMonths()));
    log.info("calculée: {} FCFA", monthlyPayment);
    log.info("   Total à rembourser: {} FCFA", totalRepayment);

    log.info("/7] Création du prêt...");
    Loan loan = Loan.builder()
        .clientId(application.getClientId())
        .clientEmail(application.getClientEmail())
        .clientFirstName(application.getClientFirstName())
        .clientLastName(application.getClientLastName())
        .amount(application.getRequestedAmount())
        .interestRate(interestRate)
        .termMonths(application.getTermMonths())
        .repaymentFrequency(RepaymentFrequency.MONTHLY)
        .monthlyPayment(monthlyPayment)
        .totalRepayment(totalRepayment)
        .remainingBalance(application.getRequestedAmount())
        .status(LoanStatus.APPROVED)
        .purpose(application.getPurpose())
        .applicationDate(application.getApplicationDate())
        .approvalDate(LocalDateTime.now())
        .approvedBy(approvedBy)
        .applicationId(application.getId())
        .build();
    
    loan = loanRepository.save(loan);
    log.info("créé avec ID: {}, Numéro: {}", loan.getId(), loan.getLoanNumber());

    log.info("/7] Mise à jour de la demande...");
    application.setStatus(ApplicationStatus.APPROVED);
    application.setReviewedDate(LocalDateTime.now());
    application.setReviewedBy(approvedBy);
    loanApplicationRepository.save(application);
    log.info("mise à jour - Statut: APPROVED, Revue par: {}", approvedBy);

    log.info("/7] Génération du plan d'amortissement...");
    amortizationService.generateAmortizationSchedule(loan);
    log.info("Plan d'amortissement genere");

    eventPublisher.publishLoanApproved(loan);
    log.info("Evenement d'approbation publie");
    
    log.info("Approbation reussie - Pret N°: {} Montant: {} FCFA", loan.getLoanNumber(), loan.getAmount());
    
    return mapToLoanResponse(loan);
}
    @Transactional
public void rejectLoan(String applicationId, String rejectionReason, String reviewedBy) {
    log.info("Rejet de la demande: {} par {}", applicationId, reviewedBy);
    
    LoanApplication application = loanApplicationRepository.findById(applicationId)
        .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
    
    if (application.getStatus() != ApplicationStatus.PENDING) {
        throw new LoanAlreadyProcessedException(applicationId, application.getStatus().name());
    }
    
    application.setStatus(ApplicationStatus.REJECTED);
    application.setRejectionReason(rejectionReason);
    application.setReviewedDate(LocalDateTime.now());
    application.setReviewedBy(reviewedBy);
    loanApplicationRepository.save(application);

    eventPublisher.publishLoanRejected(application);
    
    log.info("rejetée avec succès: {} par {}", applicationId, reviewedBy);
}

@Transactional
public LoanResponse disburseLoan(String loanId, String disbursedBy) {
    log.info("Decaissement du pret: {} par {}", loanId, disbursedBy);
    
    Loan loan = loanRepository.findById(loanId)
        .orElseThrow(() -> new RuntimeException("Prêt non trouvé"));
    
    if (loan.getStatus() != LoanStatus.APPROVED) {
        throw new RuntimeException("Le prêt n'est pas approuvé");
    }
    
    loan.setStatus(LoanStatus.ACTIVE);
    loan.setDisbursementDate(LocalDateTime.now());
    loan.setDisbursedBy(disbursedBy);  // 
    loan.setNextPaymentDate(LocalDateTime.now().plusMonths(1));
    loan.setMaturityDate(LocalDateTime.now().plusMonths(loan.getTermMonths()));
    loan = loanRepository.save(loan);

    scheduleManagementService.generateSchedules(loanId);

    eventPublisher.publishLoanDisbursed(loan);
    
    return mapToLoanResponse(loan);
}
    
    public Page<LoanApplicationResponse> getPendingApplications(Pageable pageable) {
        Page<LoanApplication> applications = loanApplicationRepository.findByStatus(ApplicationStatus.PENDING, pageable);
        return applications.map(this::mapToApplicationResponse);
    }
    
    public LoanApplicationResponse getApplication(String applicationId) {
        LoanApplication application = loanApplicationRepository.findById(applicationId)
            .orElseThrow(() -> new LoanApplicationNotFoundException(applicationId));
        return mapToApplicationResponse(application);
    }
    
    public List<LoanResponse> getClientLoans(String clientId) {
        return loanRepository.findByClientId(clientId)
            .stream()
            .map(this::mapToLoanResponse)
            .collect(Collectors.toList());
    }

    public List<LoanApplicationResponse> getClientApplications(String clientId) {
        return loanApplicationRepository.findByClientId(clientId)
            .stream()
            .map(this::mapToApplicationResponse)
            .collect(Collectors.toList());
    }
    
    public LoanResponse getLoan(String loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Prêt non trouvé"));
        return mapToLoanResponse(loan);
    }
    
    public AmortizationResponse getAmortizationSchedule(String loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Prêt non trouvé"));
        return amortizationService.getAmortizationSchedule(loan);
    }
    
    /**
     * Récupère un prêt par son ID
     */
    public Loan getLoanById(String loanId) {
        return loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Prêt non trouvé: " + loanId));
    }
    
   private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal monthlyRate, int months) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }
        
        BigDecimal factor = monthlyRate.add(BigDecimal.ONE).pow(months);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(factor);
        BigDecimal denominator = factor.subtract(BigDecimal.ONE);
        
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    /**
     * Récupère les prêts pour une liste d'IDs clients
     */
    public List<LoanResponse> getLoansByClientIds(List<String> clientIds) {
         log.info("Recuperation des prets pour {} clients", clientIds != null ? clientIds.size() : 0);

         if (clientIds == null || clientIds.isEmpty()) {
             log.warn("Liste de clients vide, retour d'une liste vide");
             return List.of();
         }
    
        try {
             List<Loan> loans = loanRepository.findByClientIdIn(clientIds);
             log.info("prêts trouvés pour {} clients", loans.size(), clientIds.size());
                    return loans.stream()
                .map(this::mapToLoanResponse)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des prêts: {}", e.getMessage());
            return List.of();
        }
    }

    public List<LoanApplicationResponse> getApplicationsByClientIds(List<String> clientIds) {
        log.info("Récupération des demandes pour {} clients", clientIds != null ? clientIds.size() : 0);
        if (clientIds == null || clientIds.isEmpty()) return List.of();
        try {
            List<LoanApplication> apps = loanApplicationRepository.findByClientIdIn(clientIds);
            return apps.stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des demandes: {}", e.getMessage());
            return List.of();
        }
    }
    
    public LoanApplicationResponse mapToApplicationResponse(LoanApplication application) {
        return LoanApplicationResponse.builder()
            .id(application.getId())
            .applicationNumber(application.getApplicationNumber())
            .clientId(application.getClientId())
            .clientEmail(application.getClientEmail())
            .clientFirstName(application.getClientFirstName())
            .clientLastName(application.getClientLastName())
            .requestedAmount(application.getRequestedAmount())
            .termMonths(application.getTermMonths())
            .purpose(application.getPurpose())
            .status(application.getStatus())
            .applicationDate(application.getApplicationDate())
            .rejectionReason(application.getRejectionReason())
            .build();
    }

 // Vérifie si un utilisateur a le droit d'approuver/rejeter une demande

private void checkApprovalAuthorization(String applicationId, String userEmail, String userRole, String token) {
    log.info(" Vérification des droits pour l'utilisateur: {} (rôle: {})", userEmail, userRole);

    // ADMIN peut tout faire (comparaison insensible à la casse)
    if (userRole != null && "ADMIN".equalsIgnoreCase(userRole.trim())) {
        log.info(" ADMIN autorisé à approuver/rejeter");
        return;
    }
    
    // Récupérer la demande de prêt
    LoanApplication application = loanApplicationRepository.findById(applicationId)
        .orElseThrow(() -> new RuntimeException("Demande non trouvée: " + applicationId));
    
    String clientId = application.getClientId();
    String accountNumber = application.getAccountNumber();  //   le compte utilisé
    
    log.info("concerné par la demande: {}", clientId);
    log.info("utilisé pour la demande: {}", accountNumber);
    
    // Récupérer l'agence à partir du compte SPÉCIFIQUE utilisé pour la demande
    String clientAgencyId = null;
    try {
        AccountInfo account = accountServiceClient.getAccountByNumber(accountNumber, token);
        if (account != null) {
            clientAgencyId = account.getAgencyId();
            log.info("Agence du compte {}: {}", accountNumber, clientAgencyId);
        } else {
            log.error("non trouvé: {}", accountNumber);
            throw new RuntimeException("Compte non trouvé: " + accountNumber);
        }
    } catch (Exception e) {
        log.error("de récupérer l'agence du compte: {}", e.getMessage());
        throw new RuntimeException("Impossible de vérifier l'agence du compte: " + e.getMessage());
    }
    
    String normalizedRole = userRole != null ? userRole.trim().toUpperCase() : "";

    if (clientAgencyId == null) {
        log.warn("Compte {} sans agence assignée — vérification d'agence ignorée pour rôle {}", accountNumber, userRole);
        if ("DIRECTEUR_AGENCE".equals(normalizedRole) || "AGENT".equals(normalizedRole)) {
            return;
        }
        throw new RuntimeException("Rôle non autorisé pour approuver/rejeter un prêt");
    }

    // Vérifier selon le rôle
    if ("DIRECTEUR_AGENCE".equals(normalizedRole)) {
        boolean isDirectorOfAgency = checkDirectorBelongsToAgency(userEmail, clientAgencyId, token);
        if (!isDirectorOfAgency) {
            throw new RuntimeException("Vous ne pouvez pas approuver/rejeter ce prêt car il ne concerne pas votre agence");
        }
        log.info(" DIRECTEUR_AGENCE autorisé pour l'agence: {}", clientAgencyId);

    } else if ("AGENT".equals(normalizedRole)) {
        boolean isAgentOfAgency = checkAgentBelongsToAgency(userEmail, clientAgencyId, token);
        if (!isAgentOfAgency) {
            throw new RuntimeException("Vous ne pouvez pas approuver/rejeter ce prêt car vous n'êtes pas assigné à l'agence du client");
        }
        log.info(" AGENT autorisé pour l'agence: {}", clientAgencyId);

    } else {
        throw new RuntimeException("Rôle non autorisé pour approuver/rejeter un prêt");
    }
}
    private boolean checkDirectorBelongsToAgency(String directorEmail, String agencyId, String token) {
        try {
            AgencyResponse agency = agencyServiceClient.getAgencyByDirectorEmail(directorEmail, token);
            return agency != null && agency.getId().equals(agencyId);
        } catch (Exception e) {
            log.error("vérification directeur: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkAgentBelongsToAgency(String agentEmail, String agencyId, String token) {
        try {
            AgentAssignmentResponse assignment = agencyServiceClient.getAgentAssignmentByEmail(agentEmail, token);
            return assignment != null && assignment.isActive() && assignment.getAgencyId().equals(agencyId);
        } catch (Exception e) {
            log.error("vérification agent: {}", e.getMessage());
            return false;
        }
    }

    @Transactional
    public LoanResponse approveLoan(String applicationId, String approvedBy, String userRole, String token, LoanApprovalRequest request) {
    log.info("║                    APPROBATION DE PRÊT                         ║");
    log.info("║ Application ID: {}", applicationId);
    log.info("║ Approuvé par   : {} (rôle: {})", approvedBy, userRole);
    
          //  SEULE VÉRIFICATION
          checkApprovalAuthorization(applicationId, approvedBy, userRole, token);
    
         // . Récupérer la demande
         LoanApplication application = loanApplicationRepository.findById(applicationId)
               .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
    
         // . Vérifier le statut
         if (application.getStatus() != ApplicationStatus.PENDING) {
              throw new LoanAlreadyProcessedException(applicationId, application.getStatus().name());
          }
    
         // . Vérifier l'éligibilité
         EligibilityResponse eligibility;
         try {
             eligibility = eligibilityService.checkEligibility(
                  application.getClientId(),
                  application.getRequestedAmount(),
                  application.getTermMonths(),
                  token
             );
         } catch (FeignException e) {
             log.error("Erreur lors de la vérification d'éligibilité (service compte): {}", e.getMessage());
             throw new RuntimeException("Compte bancaire requis: impossible de vérifier l'éligibilité du client.");
         } catch (Exception e) {
             log.error("Erreur inattendue lors de la vérification d'éligibilité: {}", e.getMessage());
             throw new RuntimeException("Compte bancaire requis: erreur lors de la vérification d'éligibilité.");
         }

         if (!eligibility.isEligible()) {
                application.setStatus(ApplicationStatus.REJECTED);
                application.setRejectionReason("Client non éligible: " + eligibility.getMessage());
                application.setReviewedDate(LocalDateTime.now());
                application.setReviewedBy(approvedBy);
                loanApplicationRepository.save(application);
                throw new RuntimeException("Client non éligible: " + eligibility.getMessage());
           }
    
           BigDecimal interestRate = request.getInterestRate() != null
               ? request.getInterestRate() 
               : new BigDecimal("15.0"); 
           String productName = "Taux standard";
    
           if (request.getProductId() != null && !request.getProductId().isEmpty()) {
               try {
                      LoanProduct loanProduct = loanProductService.getProductEntity(request.getProductId());
                      if (loanProduct != null && loanProduct.isActive()) {
                           interestRate = loanProduct.getInterestRate();
                           productName = loanProduct.getName();
                           log.info("de prêt utilisé: {} - Taux: {}%", productName, interestRate);
                
                          // que le montant est dans les limites du produit
                          BigDecimal requestedAmount = application.getRequestedAmount();
                          if (requestedAmount.compareTo(loanProduct.getMinAmount()) < 0 ||
                                requestedAmount.compareTo(loanProduct.getMaxAmount()) > 0) {
                                throw new RuntimeException(String.format(
                                "Le montant demandé (%.0f FCFA) n'est pas dans les limites du produit %s (%.0f - %.0f FCFA)",
                                requestedAmount, productName, loanProduct.getMinAmount(), loanProduct.getMaxAmount()));
                           }
                
                          Integer termMonths = application.getTermMonths();
                          if (termMonths < loanProduct.getMinTermMonths() ||
                               termMonths > loanProduct.getMaxTermMonths()) {
                               throw new RuntimeException(String.format(
                               "La durée demandée (%d mois) n'est pas dans les limites du produit %s (%d - %d mois)",
                               termMonths, productName, loanProduct.getMinTermMonths(), loanProduct.getMaxTermMonths()));
                           }
                
                        } else {
                               log.warn("non trouvé ou inactif, utilisation taux par défaut: {}%", interestRate);
                        }
                   } catch (Exception e) {
                     log.error("lors de la récupération du produit: {}", e.getMessage());
                     throw new RuntimeException("Produit de prêt invalide: " + request.getProductId());
                   }
            } else {
              log.info(" Aucun produit spécifié, utilisation du taux par défaut: {}%", interestRate);
            }
    
            // . Convertir le taux annuel en taux mensuel
            BigDecimal monthlyRate = interestRate
                .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)  // .0 -> 0.15
                . divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);  // .15 -> 0.0125
    
            // . Calculer les mensualités
            BigDecimal monthlyPayment = calculateMonthlyPayment(
               application.getRequestedAmount(),
               monthlyRate,
               application.getTermMonths()
            );
            BigDecimal totalRepayment = monthlyPayment.multiply(BigDecimal.valueOf(application.getTermMonths()));
    
            log.info("annuel: {}% - Mensualité: {} FCFA - Total à rembourser: {} FCFA", 
            interestRate, monthlyPayment, totalRepayment);
    
           // . Créer le prêt
           Loan loan = Loan.builder()
                .clientId(application.getClientId())
                .clientEmail(application.getClientEmail())
                .clientFirstName(application.getClientFirstName())
                .clientLastName(application.getClientLastName())
                .amount(application.getRequestedAmount())
                .interestRate(interestRate)
                .termMonths(application.getTermMonths())
                .repaymentFrequency(RepaymentFrequency.MONTHLY)
                .monthlyPayment(monthlyPayment)
                .totalRepayment(totalRepayment)
                .remainingBalance(totalRepayment)
                .status(LoanStatus.APPROVED)
                .purpose(application.getPurpose())
                .applicationDate(application.getApplicationDate())
                .approvalDate(LocalDateTime.now())
                .approvedBy(approvedBy)
                .reviewedBy(approvedBy)
                .applicationId(application.getId())
                .loanProductId(request.getProductId())  
                .loanProductName(productName)          
                .build();
    
           loan = loanRepository.save(loan);
    
           // . Mettre à jour la demande
           application.setStatus(ApplicationStatus.APPROVED);
           application.setReviewedDate(LocalDateTime.now());
           application.setReviewedBy(approvedBy);
           loanApplicationRepository.save(application);
    
           // . Générer le plan d'amortissement
           amortizationService.generateAmortizationSchedule(loan);
    
          //  événement
          try {
              eventPublisher.publishLoanApproved(loan);
          } catch (Exception e) {
              log.warn("Impossible de publier l'événement d'approbation (RabbitMQ?): {}", e.getMessage());
          }

          return mapToLoanResponse(loan);
    }

     // Rejeter un prêt avec vérification d'agence
    @Transactional
    public void rejectLoan(String applicationId, String rejectionReason, String reviewedBy, String userRole, String token) {
        log.info("de la demande: {} par {} (rôle: {})", applicationId, reviewedBy, userRole);
        
        //  les droits
        checkApprovalAuthorization(applicationId, reviewedBy, userRole, token);
        
        LoanApplication application = loanApplicationRepository.findById(applicationId)
            .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new LoanAlreadyProcessedException(applicationId, application.getStatus().name());
        }
        
        application.setStatus(ApplicationStatus.REJECTED);
        application.setRejectionReason(rejectionReason);
        application.setReviewedDate(LocalDateTime.now());
        application.setReviewedBy(reviewedBy);
        loanApplicationRepository.save(application);
        
        //  événement
        try {
            eventPublisher.publishLoanRejected(application);
        } catch (Exception e) {
            log.warn("Impossible de publier l'événement de rejet (RabbitMQ?): {}", e.getMessage());
        }

        log.info("rejetée avec succès: {} par {}", applicationId, reviewedBy);
    }

     // Décaisser un prêt avec vérification d'agence
    @Transactional
    public LoanResponse disburseLoan(String loanId, String disbursedBy, String userRole, String token) {
        log.info("du prêt: {} par {} (rôle: {})", loanId, disbursedBy, userRole);
    
        Loan loan = loanRepository.findById(loanId)
           .orElseThrow(() -> new RuntimeException("Prêt non trouvé"));
    
        // applicationId au lieu de findByApplicationNumber
        LoanApplication application = loanApplicationRepository.findById(loan.getApplicationId())
           .orElseThrow(() -> new RuntimeException("Demande associée non trouvée"));
    
       checkApprovalAuthorization(application.getId(), disbursedBy, userRole, token);
    
       if (loan.getStatus() != LoanStatus.APPROVED) {
           throw new RuntimeException("Le prêt n'est pas approuvé");
       }
    
       loan.setStatus(LoanStatus.ACTIVE);
       loan.setDisbursementDate(LocalDateTime.now());
       loan.setDisbursedBy(disbursedBy);
       loan.setRemainingBalance(loan.getTotalRepayment()); // Initialiser le solde au total à rembourser
       loan.setNextPaymentDate(LocalDateTime.now().plusMonths(1));
       loan.setMaturityDate(LocalDateTime.now().plusMonths(loan.getTermMonths()));
       loan = loanRepository.save(loan);
    
       //  les schedules
       scheduleManagementService.generateSchedules(loanId);
    
       //  événement
       eventPublisher.publishLoanDisbursed(loan);
    
       return mapToLoanResponse(loan);
    }
    
    
    public LoanResponse mapToLoanResponse(Loan loan) {
        BigDecimal balance = loan.getRemainingBalance();
        if (balance == null) {
            balance = loan.getTotalRepayment() != null ? loan.getTotalRepayment() : loan.getAmount();
        }

        return LoanResponse.builder()
            .id(loan.getId())
            .loanNumber(loan.getLoanNumber())
            .clientId(loan.getClientId())
            .clientEmail(loan.getClientEmail())
            .clientFirstName(loan.getClientFirstName())
            .clientLastName(loan.getClientLastName())
            .amount(loan.getAmount())
            .interestRate(loan.getInterestRate())
            .termMonths(loan.getTermMonths())
            .repaymentFrequency(loan.getRepaymentFrequency())
            .monthlyPayment(loan.getMonthlyPayment())
            .totalRepayment(loan.getTotalRepayment())
            .remainingBalance(balance)
            .status(loan.getStatus())
            .disbursementDate(loan.getDisbursementDate())
            .nextPaymentDate(loan.getNextPaymentDate())
            .maturityDate(loan.getMaturityDate())
            .amortizationSchedule(amortizationService.getScheduleEntries(loan))
            .build();
    }
    private void checkAgentBelongsToClientAgency(String applicationId, String userEmail, String userRole, String token) {
    log.info(" Vérification des droits pour l'utilisateur: {} (rôle: {})", userEmail, userRole);
    
    //  la demande
    LoanApplication application = loanApplicationRepository.findById(applicationId)
        .orElseThrow(() -> new RuntimeException("Demande non trouvée"));
    
    String clientId = application.getClientId();
    
    //  l'agence du client via Account Service
    List<AccountInfo> accounts = accountServiceClient.getAccountsByClientId(clientId, token);
    if (accounts == null || accounts.isEmpty()) {
        throw new RuntimeException("Impossible de déterminer l'agence du client");
    }
    
    String clientAgencyId = accounts.get(0).getAgencyId();
    log.info("du client: {}", clientAgencyId);
    
    if ("AGENT".equals(userRole)) {
        //  que l'agent est actif et appartient à l'agence du client
        AgentAssignmentResponse assignment = agencyServiceClient.getAgentAssignmentByEmail(userEmail, token);
        if (assignment == null || !assignment.isActive()) {
            throw new RuntimeException("Vous n'êtes pas un agent actif");
        }
        if (!assignment.getAgencyId().equals(clientAgencyId)) {
            throw new RuntimeException("Vous ne pouvez pas approuver ce prêt car il ne concerne pas votre agence");
        }
        log.info(" Agent autorisé - Agence: {}", assignment.getAgencyId());
        
    } else if ("DIRECTEUR_AGENCE".equals(userRole)) {
        //  que le directeur appartient à l'agence du client
        AgencyResponse agency = agencyServiceClient.getAgencyByDirectorEmail(userEmail, token);
        if (agency == null || !agency.getId().equals(clientAgencyId)) {
            throw new RuntimeException("Vous ne pouvez pas approuver ce prêt car il ne concerne pas votre agence");
        }
        log.info(" Directeur autorisé - Agence: {}", agency.getId());
    }
}

// Repasser une demande au statut PENDING
@Transactional
public void resetToPending(String applicationId, String resetBy, String userRole, String token) {
    log.info("de la demande {} au statut PENDING par {} (rôle: {})", applicationId, resetBy, userRole);
    
    if ("DIRECTEUR_AGENCE".equals(userRole)) {
        checkApprovalAuthorization(applicationId, resetBy, userRole, token);
    }
    
    LoanApplication application = loanApplicationRepository.findById(applicationId)
        .orElseThrow(() -> new RuntimeException("Demande non trouvée: " + applicationId));
    
    if (application.getStatus() == ApplicationStatus.PENDING) {
        throw new RuntimeException("La demande est déjà en attente (PENDING)");
    }
    
    boolean hasLinkedLoan = loanRepository.existsByApplicationId(applicationId);
    if (hasLinkedLoan && application.getStatus() == ApplicationStatus.APPROVED) {
        log.warn("Un prêt a déjà été créé pour cette demande. Reset potentiellement dangereux.");
    }
    
    // . Reset du statut
    application.setStatus(ApplicationStatus.PENDING);
    application.setRejectionReason(null);
    application.setReviewedBy(null);
    application.setReviewedDate(null);
    application.setUpdatedAt(LocalDateTime.now());
    
    loanApplicationRepository.save(application);
    
    log.info("{} repassée en PENDING avec succès par {}", applicationId, resetBy);
}
}