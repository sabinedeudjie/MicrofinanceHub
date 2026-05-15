package org.example.repaymentservice.service;

import org.example.repaymentservice.client.AgencyServiceClient;
import org.example.repaymentservice.client.ClientServiceClient;
import org.example.repaymentservice.client.LoanServiceClient;
import org.example.repaymentservice.client.TransactionServiceClient;
import org.example.repaymentservice.dto.request.RemboursementTxRequest;
import org.example.repaymentservice.dto.response.TransactionTxResponse;
import org.example.repaymentservice.dto.AgencyInfo;
import org.example.repaymentservice.dto.AgentAssignmentResponse;
import org.example.repaymentservice.dto.ClientInfo;
import org.example.repaymentservice.dto.request.AgentPaymentRequest;
import org.example.repaymentservice.dto.request.PaymentRequest;
import org.example.repaymentservice.dto.response.AmortizationResponse;
import org.example.repaymentservice.dto.response.PaymentResponse;
import org.example.repaymentservice.dto.response.RepaymentStats;
import org.example.repaymentservice.exception.BusinessException;
import org.example.repaymentservice.model.Payment;
import org.example.repaymentservice.model.Repayment;
import org.example.repaymentservice.model.Schedule;
import org.example.repaymentservice.model.enums.PaymentMethod;
import org.example.repaymentservice.model.enums.PaymentStatus;
import org.example.repaymentservice.repository.PaymentRepository;
import org.example.repaymentservice.repository.RepaymentRepository;
import org.example.repaymentservice.repository.ScheduleRepository;
import org.example.repaymentservice.util.JwtUtil;
import org.example.repaymentservice.events.RepaymentEventPublisher;
import feign.FeignException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepaymentService {

    private final PaymentRepository paymentRepository;
    private final LoanServiceClient loanServiceClient;
    private final ClientServiceClient clientServiceClient;
    private final ScheduleRepository scheduleRepository;
    private final RepaymentRepository repaymentRepository;
    private final JwtUtil jwtUtil;
    private final AgencyServiceClient agencyServiceClient;
    private final TransactionServiceClient transactionServiceClient;
    private final RepaymentEventPublisher eventPublisher;

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ============================================================
    // PAIEMENT PAR LE CLIENT LUI-MÊME
    // ============================================================

    @Transactional
    public PaymentResponse clientMakePayment(PaymentRequest request, String authorization,
            String clientIdFromToken) {
        String loanId = request.getLoanId();
        BigDecimal paymentAmount = request.getAmount();

        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║              PAIEMENT PAR CLIENT - DÉBUT                       ║");
        log.info("╠════════════════════════════════════════════════════════════════╣");
        log.info("║ Client ID     : {}", clientIdFromToken);
        log.info("║ Loan ID       : {}", loanId);
        log.info("║ Montant       : {} FCFA", paymentAmount);
        log.info("║ Date          : {}", LocalDateTime.now().format(DATE_FORMATTER));
        log.info("╚════════════════════════════════════════════════════════════════╝");

        long startTime = System.currentTimeMillis();

        try {
            // 1. Récupérer le plan d'amortissement
            log.info("[1/5] Récupération du plan d'amortissement...");
            AmortizationResponse amortization = loanServiceClient.getLoanAmortization(loanId, authorization);

            if (amortization == null || amortization.getEntries() == null || amortization.getEntries().isEmpty()) {
                log.error("Aucun plan d'amortissement trouvé pour loanId: {}", loanId);
                throw BusinessException.noScheduleFound();
            }
            log.info("Plan d'amortissement récupéré: {} échéances", amortization.getEntries().size());

            // 2. Vérifier que le client est bien le propriétaire du prêt
            log.info("[2/5] Vérification de la propriété du prêt...");
            String loanClientId = getClientId(loanId, authorization);
            log.info("   Client ID du prêt: {}", loanClientId);
            log.info("   Client ID du token: {}", clientIdFromToken);

            if (!clientIdFromToken.equals(loanClientId)) {
                log.error("Tentative de paiement sur un prêt qui n'appartient pas au client");
                throw BusinessException.unauthorized("Vous ne pouvez payer que vos propres prêts");
            }
            log.info("Client autorisé - propriétaire du prêt vérifié");

            // 3. Vérifier le statut du prêt
            log.info("[3/5] Vérification du statut du prêt...");
            String loanStatus = loanServiceClient.getLoanStatus(loanId, authorization);
            log.info("   Statut du prêt: {}", loanStatus);

            if (!"ACTIVE".equals(loanStatus)) {
                log.error("Prêt non actif - statut: {}", loanStatus);
                throw BusinessException.loanNotActive(loanStatus);
            }
            log.info("Prêt actif - paiement autorisé");

            // 4. Pour MOBILE_MONEY : initier CamPay et retourner PENDING
            if (PaymentMethod.MOBILE_MONEY.equals(request.getPaymentMethod())) {
                if (request.getNumeroPaiement() == null || request.getNumeroPaiement().isBlank()) {
                    throw BusinessException.unauthorized("Le numéro Mobile Money est requis");
                }
                log.info("[4/5] Initiation Mobile Money via transaction-service...");
                return processMobileMoneyClientPayment(loanId, paymentAmount, amortization,
                        authorization, request.getNumeroPaiement(), request.getNotes(), clientIdFromToken);
            }

            // 5. Autres modes : appel transaction-service puis traitement synchrone
            String registeredBy = clientIdFromToken + " (client)";
            log.info("[5/5] Traitement du paiement...");
            PaymentResponse response = processPayment(loanId, paymentAmount, amortization, authorization,
                    request.getPaymentMethod(), request.getNotes(), registeredBy, request.getNumeroPaiement(),
                    request.getCompteSourceId());

            long duration = System.currentTimeMillis() - startTime;
            log.info("╔════════════════════════════════════════════════════════════════╗");
            log.info("║              PAIEMENT PAR CLIENT - SUCCÈS                     ║");
            log.info("║ Durée: {} ms", duration);
            log.info("║ Paiement N°: {}", response.getPaymentNumber());
            log.info("║ Échéances restantes: {}", response.getRemainingInstallments());
            log.info("╚════════════════════════════════════════════════════════════════╝");

            return response;

        } catch (BusinessException | FeignException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur technique: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors du paiement: " + e.getMessage(), e);
        }
    }

    /**
     * Valide un paiement qui était en attente
     */
    @Transactional
    public PaymentResponse validatePayment(String paymentId, String authorization, String userId, String userRole) {
        log.info("Validation du paiement {} par {} ({})", paymentId, userId, userRole);

        // 1. Vérification des droits
        if (!"ADMIN".equalsIgnoreCase(userRole) && !"DIRECTEUR_AGENCE".equalsIgnoreCase(userRole)) {
            throw BusinessException
                    .unauthorized("Seuls les administrateurs et directeurs peuvent valider les paiements");
        }

        // 2. Récupérer le paiement
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException("PAIEMENT_NOT_FOUND", "Paiement non trouvé"));

        if (payment.getStatus() != PaymentStatus.PENDING_VALIDATION) {
            throw new BusinessException("INVALID_STATUS", "Ce paiement n'est pas en attente de validation");
        }

        // 3. Vérification des droits d'agence pour le directeur
        if ("DIRECTEUR_AGENCE".equalsIgnoreCase(userRole)) {
            String validatorEmail = extractEmailFromToken(authorization);
            checkAgencyAuthorization(payment.getClientId(), validatorEmail, userRole, authorization);
        }

        // 4. Récupérer le plan d'amortissement actuel
        AmortizationResponse amortization = loanServiceClient.getLoanAmortization(payment.getLoanId(), authorization);

        // 5. Exécuter le traitement réel (Transaction financière + Loan Service)
        String validatorName = extractAgentName(authorization, userId);
        String notes = (payment.getNotes() != null ? payment.getNotes() : "") + " (Validé par " + validatorName + ")";

        PaymentResponse response = processPayment(
                payment.getLoanId(),
                payment.getAmount(),
                amortization,
                authorization,
                payment.getPaymentMethod(),
                notes,
                payment.getPaidBy(), // On garde le nom de celui qui a encaissé
                payment.getNumeroPaiement(),
                payment.getCompteSourceId());

        // 6. Mettre à jour le record original
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setValidatedBy(validatorName);
        payment.setValidatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return response;
    }

    /**
     * Récupère la liste des paiements en attente de validation
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPendingPayments(String authorization) {
        log.info("Récupération des paiements en attente de validation");
        return paymentRepository.findByStatus(PaymentStatus.PENDING_VALIDATION).stream()
                .map(p -> PaymentResponse.builder()
                        .id(p.getId())
                        .paymentNumber(p.getPaymentNumber())
                        .loanId(p.getLoanId())
                        .clientId(p.getClientId())
                        .clientName(getClientName(p.getClientId(), authorization))
                        .amount(p.getAmount())
                        .totalAmount(p.getAmount())
                        .paymentMethod(p.getPaymentMethod())
                        .status(p.getStatus())
                        .receiptNumber(p.getReceiptNumber())
                        .paymentDate(p.getPaymentDate())
                        .registeredBy(p.getPaidBy())
                        .notes(p.getNotes())
                        .build())
                .collect(Collectors.toList());
    }

    // ============================================================
    // ENREGISTREMENT DE PAIEMENT PAR AGENT/ADMIN
    // ============================================================

    @Transactional
    public PaymentResponse agentRecordPayment(AgentPaymentRequest request, String authorization,
            String agentId, String agentRole) {

        String loanId = request.getLoanId();
        String clientId = request.getClientId();
        BigDecimal paymentAmount = request.getAmount();

        String agentEmail = extractEmailFromToken(authorization);

        // Fallback si l'email n'est pas trouvé
        if (agentEmail == null || agentEmail.isEmpty()) {
            agentEmail = agentId;
            log.warn("Impossible d'extraire l'email du token, utilisation de l'agentId: {}", agentId);
        }

        String agentFullName = extractAgentName(authorization, agentId);

        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║         ENREGISTREMENT PAIEMENT PAR {} - DÉBUT            ║", agentRole);
        log.info("╠════════════════════════════════════════════════════════════════╣");
        log.info("║ Agent          : {} ({})", agentFullName, agentRole);
        log.info("║ Agent Email    : {}", agentEmail);
        log.info("║ Loan ID        : {}", loanId);
        log.info("║ Client ID      : {}", clientId);
        log.info("║ Montant        : {} FCFA", paymentAmount);
        log.info("╚════════════════════════════════════════════════════════════════╝");

        long startTime = System.currentTimeMillis();

        try {
            // 1. Vérifier que l'utilisateur a le droit d'enregistrer des paiements
            log.info("[1/7] Vérification des droits...");
            if (!"AGENT".equalsIgnoreCase(agentRole) && !"ADMIN".equalsIgnoreCase(agentRole)
                    && !"DIRECTEUR_AGENCE".equalsIgnoreCase(agentRole)) {
                log.error("Rôle non autorisé: {}", agentRole);
                throw BusinessException.unauthorized(
                        "Seuls les agents, directeurs et administrateurs peuvent enregistrer des paiements");
            }
            log.info("Rôle autorisé: {}", agentRole);

            // 2. Récupérer le plan d'amortissement
            log.info("[2/7] Récupération du plan d'amortissement...");
            AmortizationResponse amortization = loanServiceClient.getLoanAmortization(loanId, authorization);

            if (amortization == null || amortization.getEntries() == null || amortization.getEntries().isEmpty()) {
                log.error("Aucun plan d'amortissement trouvé pour loanId: {}", loanId);
                throw BusinessException.noScheduleFound();
            }
            log.info("Plan d'amortissement récupéré: {} échéances", amortization.getEntries().size());

            // 3. Vérifier que le client existe et correspond au prêt
            log.info("[3/7] Vérification de la correspondance client/prêt...");
            String loanClientId = getClientId(loanId, authorization);
            log.info("   Client ID du prêt: {}", loanClientId);
            log.info("   Client ID fourni: {}", clientId);

            if (!clientId.equals(loanClientId)) {
                log.error("Le client spécifié ne correspond pas au propriétaire du prêt");
                throw BusinessException.invalidClientForLoan(clientId, loanId);
            }
            log.info("Client vérifié - correspond au prêt");

            // 4. Vérifier que l'agent/directeur a le droit d'agir pour ce client
            // (appartenance à l'agence)
            log.info("[4/7] Vérification des droits d'agence...");
            checkAgencyAuthorization(clientId, agentEmail, agentRole, authorization);
            log.info("Autorisation d'agence vérifiée pour {}: {}", agentRole, agentEmail);

            // 5. Vérifier le statut du prêt
            log.info("[5/7] Vérification du statut du prêt...");
            String loanStatus = loanServiceClient.getLoanStatus(loanId, authorization);
            log.info("   Statut du prêt: {}", loanStatus);

            if (!"ACTIVE".equals(loanStatus)) {
                log.error("Prêt non actif - statut: {}", loanStatus);
                throw BusinessException.loanNotActive(loanStatus);
            }
            log.info("Prêt actif - paiement autorisé");

            // 6. Traiter le paiement
            String notes = buildAgentNotesWithName(request.getNotes(), agentFullName, agentId, agentRole,
                    request.getReceiptNumber());
            log.info("[6/7] Traitement du paiement...");

            // AGENT et DIRECTEUR_AGENCE : paiement en attente de validation par l'admin
            if ("AGENT".equalsIgnoreCase(agentRole) || "DIRECTEUR_AGENCE".equalsIgnoreCase(agentRole)) {
                log.info("Rôle AGENT : Enregistrement en attente de validation");

                Payment payment = Payment.builder()
                        .loanId(loanId)
                        .clientId(clientId)
                        .amount(paymentAmount)
                        .penaltyAmount(BigDecimal.ZERO)
                        .paymentMethod(request.getPaymentMethod())
                        .numeroPaiement(request.getNumeroPaiement())
                        .compteSourceId(request.getCompteSourceId())
                        .receiptNumber(request.getReceiptNumber())
                        .status(PaymentStatus.PENDING_VALIDATION)
                        .paymentDate(LocalDateTime.now())
                        .paidBy(agentFullName)
                        .notes(notes != null ? notes : "EN ATTENTE DE VALIDATION")
                        .build();

                payment = paymentRepository.save(payment);
                log.info("Paiement enregistré en attente (ID: {})", payment.getId());

                return PaymentResponse.builder()
                        .id(payment.getId())
                        .paymentNumber(payment.getPaymentNumber())
                        .loanId(loanId)
                        .clientId(clientId)
                        .clientName(getClientName(clientId, authorization))
                        .amount(paymentAmount)
                        .totalAmount(paymentAmount)
                        .paymentMethod(request.getPaymentMethod())
                        .status(PaymentStatus.PENDING_VALIDATION)
                        .message("Remboursement enregistré. En attente de validation par l'administrateur.")
                        .registeredBy(agentFullName)
                        .notes(payment.getNotes())
                        .build();
            }

            // Si c'est un ADMIN ou DIRECTEUR, on traite immédiatement
            PaymentResponse response = processPayment(loanId, paymentAmount, amortization, authorization,
                    request.getPaymentMethod(), notes, agentFullName, request.getNumeroPaiement(),
                    request.getCompteSourceId());

            long duration = System.currentTimeMillis() - startTime;
            log.info("╔════════════════════════════════════════════════════════════════╗");
            log.info("║         ENREGISTREMENT PAIEMENT PAR {} - SUCCÈS          ║", agentRole);
            log.info("║ Durée: {} ms", duration);
            log.info("║ Paiement N°: {}", response.getPaymentNumber());
            log.info("║ Échéances restantes: {}", response.getRemainingInstallments());
            log.info("╚════════════════════════════════════════════════════════════════╝");

            return response;

        } catch (BusinessException | FeignException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur technique: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'enregistrement du paiement: " + e.getMessage(), e);
        }
    }

    /**
     * Extrait l'email du token JWT
     */
    private String extractEmailFromToken(String authorization) {
        try {
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7);
                return jwtUtil.extractEmail(token);
            }
        } catch (Exception e) {
            log.warn("Impossible d'extraire l'email du token: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Vérifie que l'agent/directeur a le droit d'agir pour le client
     * (vérifie l'appartenance à la même agence)
     */
    private void checkAgencyAuthorization(String clientId, String userEmail, String userRole, String authorization) {
        log.info("Vérification des droits d'agence pour l'utilisateur: {} (rôle: {})", userEmail, userRole);

        try {
            // 1. Récupérer l'agence du client
            // 1. Si ADMIN, on autorise directement sans même chercher l'agence
            if ("ADMIN".equalsIgnoreCase(userRole)) {
                log.info("ADMIN autorisé - pas de restriction d'agence");
                return;
            }

            // 2. Récupérer l'agence du client
            AgencyInfo clientAgency = agencyServiceClient.getClientAgency(clientId, authorization);
            if (clientAgency == null || clientAgency.getId() == null) {
                log.error("Impossible de trouver l'agence du client: {}", clientId);
                throw BusinessException.unauthorized("Impossible de déterminer l'agence du client");
            }
            log.info("Agence du client: {} ({})", clientAgency.getCode(), clientAgency.getId());

            // 3. Vérification pour les autres rôles
            if ("DIRECTEUR_AGENCE".equalsIgnoreCase(userRole)) {
                // Vérifier que le directeur appartient à l'agence du client
                AgencyInfo directorAgency = agencyServiceClient.getAgencyByDirectorEmail(userEmail, authorization);
                if (directorAgency == null || directorAgency.getId() == null) {
                    log.error("Directeur non trouvé ou non associé à une agence: {}", userEmail);
                    throw BusinessException.unauthorized("Vous n'êtes pas associé à une agence");
                }

                if (!directorAgency.getId().equals(clientAgency.getId())) {
                    log.error("{} de l'agence {} tente de payer pour un client de l'agence {}",
                            userEmail, directorAgency.getCode(), clientAgency.getCode());
                    throw BusinessException.unauthorized(
                            "Vous ne pouvez enregistrer un paiement que pour les clients de votre agence");
                }
                log.info("Directeur autorisé - agence: {}", directorAgency.getCode());

            } else if ("AGENT".equalsIgnoreCase(userRole)) {
                // Vérifier que l'agent est actif et appartient à l'agence du client
                AgentAssignmentResponse agentAssignment = agencyServiceClient.getAgentAssignmentByEmail(userEmail,
                        authorization);
                if (agentAssignment == null) {
                    log.error("Agent non trouvé ou non assigné: {}", userEmail);
                    throw BusinessException.unauthorized("Vous n'êtes pas assigné à une agence");
                }

                if (!agentAssignment.isActive()) {
                    log.error("Agent inactif: {}", userEmail);
                    throw BusinessException.unauthorized("Votre compte agent est inactif");
                }

                if (!agentAssignment.getAgencyId().equals(clientAgency.getId())) {
                    log.error("{} de l'agence {} tente de payer pour un client de l'agence {}",
                            userEmail, agentAssignment.getAgencyCode(), clientAgency.getCode());
                    throw BusinessException.unauthorized(
                            "Vous ne pouvez enregistrer un paiement que pour les clients de votre agence");
                }
                log.info("Agent autorisé - agence: {}", agentAssignment.getAgencyCode());

            } else {
                log.error("Rôle non reconnu: {}", userRole);
                throw BusinessException.unauthorized("Rôle non autorisé pour enregistrer des paiements");
            }

        } catch (FeignException.NotFound e) {
            log.warn("L'agence ou le client n'a pas pu être trouvé lors de la vérification : {}", e.getMessage());
            // Pour un AGENT, si on ne trouve pas l'agence du client, on laisse passer avec
            // un warning
            // car le paiement sera validé par un supérieur de toute façon.
            if ("AGENT".equalsIgnoreCase(userRole) || "DIRECTEUR_AGENCE".equalsIgnoreCase(userRole)) {
                log.warn("Autorisation accordée par défaut à {} {} car l'agence du client est introuvable (404)",
                        userRole, userEmail);
                return;
            }
            throw BusinessException.unauthorized("Impossible de vérifier vos droits d'accès : Agence introuvable");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification des droits d'agence: {}", e.getMessage());
            throw BusinessException.unauthorized("Impossible de vérifier vos droits d'accès");
        }
    }

    /**
     * Extrait le nom complet de l'agent depuis le token JWT
     */
    private String extractAgentName(String authorization, String agentId) {
        try {
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7);
                String fullName = jwtUtil.extractFullName(token);
                if (fullName != null && !fullName.trim().isEmpty()) {
                    return fullName;
                }
            }
        } catch (Exception e) {
            log.warn("Impossible d'extraire le nom du token: {}", e.getMessage());
        }

        // Fallback : utiliser l'ID
        return "Agent " + (agentId.length() > 8 ? agentId.substring(0, 8) : agentId);
    }

    /**
     * Construit les notes avec les informations de l'agent
     */
    private String buildAgentNotesWithName(String originalNotes, String agentName, String agentId,
            String agentRole, String receiptNumber) {
        StringBuilder notesBuilder = new StringBuilder();

        if (originalNotes != null && !originalNotes.isEmpty()) {
            notesBuilder.append(originalNotes);
            notesBuilder.append(" | ");
        }

        notesBuilder.append("Enregistré par: ").append(agentName);
        notesBuilder.append(" (").append(agentRole).append(")");

        if (receiptNumber != null && !receiptNumber.isEmpty()) {
            notesBuilder.append(" | Reçu N°: ").append(receiptNumber);
        }

        notesBuilder.append(" | Date: ").append(LocalDateTime.now().format(DATE_FORMATTER));

        return notesBuilder.toString();
    }

    // ============================================================
    // MÉTHODES COMMUNES DE TRAITEMENT
    // ============================================================

    private PaymentResponse processMobileMoneyClientPayment(String loanId, BigDecimal paymentAmount,
            AmortizationResponse amortization, String authorization,
            String numeroPaiement, String notes, String clientId) {

        log.info("[Mobile Money] Initiation du remboursement pour loanId={}, montant={}", loanId, paymentAmount);

        // Construire la requête vers transaction-service
        RemboursementTxRequest txRequest = new RemboursementTxRequest();
        txRequest.setLoanId(loanId);
        txRequest.setClientId(clientId);
        txRequest.setMontant(paymentAmount);
        txRequest.setModePaiement("MOBILE_MONEY");
        txRequest.setNumeroPaiement(numeroPaiement);
        txRequest.setDescription("Remboursement Mobile Money - prêt " + loanId);

        // Appel au transaction-service
        TransactionTxResponse txResponse = transactionServiceClient.enregistrerRemboursement(txRequest, authorization);

        log.info("[Mobile Money] Transaction initiée : réf={}, statut={}",
                txResponse.getReference(), txResponse.getStatut());

        // Enregistrer un Payment local en PENDING
        String clientName = getClientName(clientId, authorization);

        Payment payment = Payment.builder()
                .paymentNumber("PAY" + System.currentTimeMillis())
                .loanId(loanId)
                .clientId(clientId)
                .amount(paymentAmount)
                .penaltyAmount(BigDecimal.ZERO)
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .numeroPaiement(numeroPaiement)
                .status(PaymentStatus.PENDING)
                .paymentDate(LocalDateTime.now())
                .paidBy(clientId + " (client)")
                .notes(notes != null ? notes : "Remboursement Mobile Money en attente")
                .build();

        payment = paymentRepository.save(payment);
        log.info("[Mobile Money] Payment local créé en PENDING : {}", payment.getPaymentNumber());

        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .loanId(loanId)
                .clientId(clientId)
                .clientName(clientName)
                .amount(paymentAmount)
                .penaltyAmount(BigDecimal.ZERO)
                .totalAmount(paymentAmount)
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .status(PaymentStatus.PENDING)
                .paymentDate(payment.getPaymentDate())
                .message("Paiement Mobile Money initié. Veuillez confirmer sur votre téléphone (réf: "
                        + txResponse.getReference() + ")")
                .build();
    }

    private PaymentResponse processPayment(String loanId, BigDecimal paymentAmount,
            AmortizationResponse amortization, String authorization,
            PaymentMethod paymentMethod, String notes, String registeredBy,
            String numeroPaiement, Long compteSourceId) {

        log.info("┌─────────────────── TRAITEMENT DU PAIEMENT ───────────────────────┐");

        // 0. Enregistrer la transaction financière (Débit si virement, simple record si
        // espèces/chèque)
        String clientId = getClientId(loanId, authorization);
        String paymentNumber = "PAY" + System.currentTimeMillis();

        log.info("│ 0. Enregistrement de la transaction financière...");
        RemboursementTxRequest txRequest = RemboursementTxRequest.builder()
                .loanId(loanId)
                .clientId(clientId)
                .montant(paymentAmount)
                .modePaiement(mapPaymentMethodToModePaiement(paymentMethod))
                .numeroPaiement(numeroPaiement)
                .compteSourceId(compteSourceId)
                .description(notes != null ? notes : "Remboursement prêt " + loanId)
                .referenceRepayment(paymentNumber)
                .build();

        try {
            transactionServiceClient.enregistrerRemboursement(txRequest, authorization);
            log.info("│    Transaction financière enregistrée avec succès");
        } catch (BusinessException | FeignException e) {
            throw e;
        } catch (Exception e) {
            log.error("│    ÉCHEC de l'enregistrement de la transaction financière: {}", e.getMessage());
            throw new RuntimeException("Impossible d'effectuer le remboursement: " + e.getMessage());
        }

        // 1. Récupérer les échéances impayées
        log.info("│ 1. Analyse des échéances...");
        List<AmortizationResponse.AmortizationEntry> unpaidEntries = new ArrayList<>();
        for (AmortizationResponse.AmortizationEntry entry : amortization.getEntries()) {
            if (!entry.isPaid()) {
                unpaidEntries.add(entry);
                log.info("│    - Échéance {}: {} FCFA (non payée)",
                        entry.getInstallmentNumber(), entry.getDueAmount());
            }
        }

        if (unpaidEntries.isEmpty()) {
            log.error("│ Aucune échéance impayée trouvée");
            throw BusinessException.loanAlreadyPaid();
        }
        log.info("│    Total échéances impayées: {}", unpaidEntries.size());

        // 2. Calculer le montant total restant
        BigDecimal totalRemaining = unpaidEntries.stream()
                .map(AmortizationResponse.AmortizationEntry::getDueAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("│ 2. Montant total restant: {} FCFA", totalRemaining);

        // 3. Vérifier si le paiement couvre tout le prêt
        boolean payFullLoan = paymentAmount.compareTo(totalRemaining) >= 0;
        log.info("│ 3. Type de paiement: {}", payFullLoan ? "INTÉGRAL" : "PARTIEL");

        PaymentResponse response;
        if (payFullLoan) {
            log.info("│ 4. Traitement paiement intégral...");
            response = processFullLoanPayment(loanId, paymentAmount, totalRemaining, unpaidEntries,
                    amortization, authorization, paymentMethod, notes, registeredBy, numeroPaiement, paymentNumber);
        } else {
            log.info("│ 4. Traitement paiement partiel...");
            response = processPartialPayment(loanId, paymentAmount, unpaidEntries, amortization,
                    authorization, paymentMethod, notes, registeredBy, numeroPaiement, paymentNumber);
        }

        log.info("└─────────────────────────────────────────────────────────────────┘");
        return response;
    }

    /**
     * Traite un paiement intégral (remboursement total du prêt)
     */
    private PaymentResponse processFullLoanPayment(String loanId, BigDecimal paymentAmount,
            BigDecimal totalRemaining, List<AmortizationResponse.AmortizationEntry> unpaidEntries,
            AmortizationResponse amortization, String authorization, PaymentMethod paymentMethod,
            String notes, String registeredBy, String numeroPaiement, String paymentNumber) {

        BigDecimal overpayment = paymentAmount.subtract(totalRemaining);

        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║                    PAIEMENT INTÉGRAL                           ║");
        log.info("╠════════════════════════════════════════════════════════════════╣");
        log.info("║ Montant payé    : {} FCFA", paymentAmount);
        log.info("║ Montant dû      : {} FCFA", totalRemaining);
        log.info("║ Trop-perçu      : {} FCFA", overpayment);
        log.info("║ Méthode         : {}", paymentMethod);
        log.info("║ Enregistré par  : {}", registeredBy);
        log.info("╚════════════════════════════════════════════════════════════════╝");

        // Récupérer les informations client
        String clientId = getClientId(loanId, authorization);
        String clientName = getClientName(clientId, authorization);
        log.info("Client: {} ({})", clientName, clientId);

        // Créer le paiement
        Payment payment = Payment.builder()
                .paymentNumber(paymentNumber)
                .loanId(loanId)
                .clientId(clientId)
                .amount(paymentAmount)
                .penaltyAmount(BigDecimal.ZERO)
                .paymentMethod(paymentMethod)
                .numeroPaiement(numeroPaiement) // FIX: ajout du numeroPaiement
                .status(PaymentStatus.COMPLETED)
                .paymentDate(LocalDateTime.now())
                .paidBy(registeredBy)
                .notes(notes != null ? notes : "PAIEMENT INTÉGRAL")
                .build();

        payment = paymentRepository.save(payment);
        log.info("Paiement enregistré: {}", payment.getPaymentNumber());

        // Créer les repayments pour toutes les échéances payées
        log.info("Création des repayments pour {} échéances", unpaidEntries.size());
        createRepayments(payment, loanId, clientId, unpaidEntries);

        // Marquer TOUTES les échéances comme payées
        log.info("Marquage des échéances...");
        for (AmortizationResponse.AmortizationEntry entry : unpaidEntries) {
            try {
                String schedulePaymentId = UUID.randomUUID().toString();
                loanServiceClient.markScheduleAsPaid(loanId, entry.getInstallmentNumber(),
                        schedulePaymentId, authorization);
                log.info("   Échéance {} marquée comme payée (paymentId: {})",
                        entry.getInstallmentNumber(), schedulePaymentId);
            } catch (Exception e) {
                log.error("   Erreur pour l'échéance {}: {}", entry.getInstallmentNumber(), e.getMessage());
            }
        }

        // Mettre à jour les schedules locaux
        updateLocalSchedules(loanId, unpaidEntries);

        log.info("Prêt ENTIÈREMENT REMBOURSÉ !");

        PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .loanId(payment.getLoanId())
                .clientId(payment.getClientId())
                .clientName(clientName)
                .amount(payment.getAmount())
                .penaltyAmount(payment.getPenaltyAmount())
                .totalAmount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .paymentDate(payment.getPaymentDate())
                .remainingInstallments(0)
                .remainingBalance(BigDecimal.ZERO)
                .registeredBy(registeredBy)
                .message("Prêt entièrement remboursé !");

        // Publier l'événement de paiement reçu
        try {
            ClientInfo clientInfo = clientServiceClient.getClientInfo(clientId, authorization);
            String email = (clientInfo != null) ? clientInfo.getEmail() : null;
            String nom = (clientInfo != null) ? clientInfo.getFirstName() + " " + clientInfo.getLastName() : null;

            // On récupère les schedules correspondants aux entries payées
            List<Schedule> schedules = unpaidEntries.stream()
                    .map(entry -> scheduleRepository.findByLoanIdAndInstallmentNumber(loanId,
                            entry.getInstallmentNumber()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            eventPublisher.publishPaymentReceived(payment, schedules, email, nom);
        } catch (Exception e) {
            log.warn("Impossible de publier l'événement PaymentReceived: {}", e.getMessage());
        }

        if (overpayment.compareTo(BigDecimal.ZERO) > 0) {
            builder.overpayment(overpayment);
        }

        return builder.build();
    }

    /**
     * Traite un paiement partiel (une ou plusieurs échéances)
     * FIX: ajout du paramètre numeroPaiement
     */
    private PaymentResponse processPartialPayment(String loanId, BigDecimal paymentAmount,
            List<AmortizationResponse.AmortizationEntry> unpaidEntries,
            AmortizationResponse amortization, String authorization, PaymentMethod paymentMethod,
            String notes, String registeredBy, String numeroPaiement, String paymentNumber) {

        BigDecimal remainingAmount = paymentAmount;
        List<Integer> paidInstallments = new ArrayList<>();

        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║                    PAIEMENT PARTIEL                             ║");
        log.info("╠════════════════════════════════════════════════════════════════╣");
        log.info("║ Montant payé    : {} FCFA", paymentAmount);
        log.info("║ Méthode         : {}", paymentMethod);
        log.info("║ Enregistré par  : {}", registeredBy);
        log.info("╠════════════════════════════════════════════════════════════════╣");

        // Récupérer les informations client
        String clientId = getClientId(loanId, authorization);
        String clientName = getClientName(clientId, authorization);

        // Parcourir les échéances et payer autant que possible
        log.info("│ Traitement des échéances:                                       │");
        for (AmortizationResponse.AmortizationEntry entry : unpaidEntries) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("│    Arrêt - plus de fonds disponibles");
                break;
            }

            BigDecimal dueAmount = entry.getDueAmount();
            log.info("│    Échéance {}: due={} FCFA, reste={} FCFA",
                    entry.getInstallmentNumber(), dueAmount, remainingAmount);

            // Tolérance de 1 FCFA pour les arrondis (très important pour FCFA avec
            // décimales)
            BigDecimal tolerance = new BigDecimal("1.0");

            if (remainingAmount.add(tolerance).compareTo(dueAmount) >= 0) {
                // Si le montant restant est supérieur ou presque égal au dû
                BigDecimal amountToSubtract = remainingAmount.compareTo(dueAmount) >= 0 ? dueAmount : remainingAmount;
                remainingAmount = remainingAmount.subtract(amountToSubtract);

                paidInstallments.add(entry.getInstallmentNumber());

                // Générer un ID unique pour cette échéance
                String schedulePaymentId = UUID.randomUUID().toString();

                try {
                    loanServiceClient.markScheduleAsPaid(loanId, entry.getInstallmentNumber(),
                            schedulePaymentId, authorization);
                    log.info("│    Échéance {} payée (ID: {})",
                            entry.getInstallmentNumber(), schedulePaymentId);
                } catch (Exception e) {
                    log.error("│    Erreur échéance {}: {}", entry.getInstallmentNumber(), e.getMessage());
                    throw new RuntimeException("Erreur lors du paiement de l'échéance " + entry.getInstallmentNumber(),
                            e);
                }

                // Mettre à jour la table schedules locale
                updateLocalSchedule(loanId, entry.getInstallmentNumber(), schedulePaymentId);

            } else {
                // Si le montant restant est vraiment trop petit par rapport à l'échéance,
                // on ne jette pas d'erreur, on considère juste que c'est un trop-perçu minime
                if (remainingAmount.compareTo(tolerance) < 0) {
                    log.info("│    Arrêt - montant restant négligeable ({} < {})", remainingAmount, tolerance);
                    break;
                }

                log.warn("│    Paiement partiel non autorisé: {} < {}", remainingAmount, dueAmount);
                throw BusinessException.partialPaymentNotAllowed(dueAmount, remainingAmount);
            }
        }

        BigDecimal overpayment = remainingAmount;
        log.info("╠════════════════════════════════════════════════════════════════╣");
        log.info("║ Échéances payées: {}", paidInstallments);
        log.info("║ Trop-perçu      : {} FCFA", overpayment);
        log.info("╚════════════════════════════════════════════════════════════════╝");

        // Enregistrer le paiement
        Payment payment = Payment.builder()
                .paymentNumber(paymentNumber)
                .loanId(loanId)
                .clientId(clientId)
                .amount(paymentAmount)
                .penaltyAmount(BigDecimal.ZERO)
                .paymentMethod(paymentMethod)
                .numeroPaiement(numeroPaiement) // FIX: ajout du numeroPaiement
                .status(PaymentStatus.COMPLETED)
                .paymentDate(LocalDateTime.now())
                .paidBy(registeredBy)
                .notes(notes != null ? notes : "Paiement de " + paidInstallments.size() + " échéances")
                .build();

        payment = paymentRepository.save(payment);
        log.info("Paiement enregistré: {} - Montant: {} FCFA", payment.getPaymentNumber(), payment.getAmount());

        // Créer les repayments pour les échéances payées
        List<AmortizationResponse.AmortizationEntry> paidEntries = unpaidEntries.stream()
                .filter(e -> paidInstallments.contains(e.getInstallmentNumber()))
                .collect(Collectors.toList());

        log.info("Création des repayments pour {} échéances", paidEntries.size());
        createRepayments(payment, loanId, clientId, paidEntries);

        // Attendre un peu pour que la base de données soit mise à jour
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Récupérer les informations après paiement
        log.info("Vérification du statut après paiement...");
        AmortizationResponse updatedAmortization = loanServiceClient.getLoanAmortization(loanId, authorization);

        long remainingInstallments = updatedAmortization.getEntries().stream()
                .filter(entry -> !entry.isPaid())
                .count();

        BigDecimal newRemainingBalance = updatedAmortization.getEntries().stream()
                .filter(entry -> !entry.isPaid())
                .map(AmortizationResponse.AmortizationEntry::getRemainingBalance)
                .findFirst()
                .orElse(BigDecimal.ZERO);

        log.info("Résultat final:");
        log.info("   - Échéances restantes: {}", remainingInstallments);
        log.info("   - Solde restant: {} FCFA", newRemainingBalance);

        String successMessage = paidInstallments.size() + " échéance(s) payée(s)";

        PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .loanId(payment.getLoanId())
                .clientId(payment.getClientId())
                .clientName(clientName)
                .amount(payment.getAmount())
                .penaltyAmount(payment.getPenaltyAmount())
                .totalAmount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .paymentDate(payment.getPaymentDate())
                .remainingInstallments((int) remainingInstallments)
                .remainingBalance(newRemainingBalance)
                .paidInstallments(paidInstallments)
                .registeredBy(registeredBy)
                .message(successMessage);

        // Publier l'événement de paiement reçu
        try {
            ClientInfo clientInfo = clientServiceClient.getClientInfo(clientId, authorization);
            String email = (clientInfo != null) ? clientInfo.getEmail() : null;
            String nom = (clientInfo != null) ? clientInfo.getFirstName() + " " + clientInfo.getLastName() : null;

            List<Schedule> schedules = paidEntries.stream()
                    .map(entry -> scheduleRepository.findByLoanIdAndInstallmentNumber(loanId,
                            entry.getInstallmentNumber()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            eventPublisher.publishPaymentReceived(payment, schedules, email, nom);
        } catch (Exception e) {
            log.warn("Impossible de publier l'événement PaymentReceived: {}", e.getMessage());
        }

        if (overpayment.compareTo(BigDecimal.ZERO) > 0) {
            builder.overpayment(overpayment);
            builder.message(successMessage + ". Trop-perçu: " + overpayment + " FCFA");
        }

        return builder.build();
    }

    // ============================================================
    // MÉTHODES UTILITAIRES
    // ============================================================

    /**
     * Crée les enregistrements dans la table repayments
     */
    private void createRepayments(Payment payment, String loanId, String clientId,
            List<AmortizationResponse.AmortizationEntry> paidEntries) {

        if (paidEntries == null || paidEntries.isEmpty()) {
            log.warn("paidEntries est vide, aucun repayment créé");
            return;
        }

        log.info("createRepayments - loanId: {}, clientId: {}, entries: {}", loanId, clientId, paidEntries.size());

        List<Repayment> repayments = new ArrayList<>();

        for (AmortizationResponse.AmortizationEntry entry : paidEntries) {
            Optional<Repayment> existing = repaymentRepository.findByLoanIdAndInstallmentNumber(loanId,
                    entry.getInstallmentNumber());

            if (existing.isPresent()) {
                log.info("Repayment existe déjà pour échéance {}, mise à jour", entry.getInstallmentNumber());
                Repayment rep = existing.get();
                rep.setPaidAmount(entry.getDueAmount());
                rep.setStatus(PaymentStatus.COMPLETED);
                rep.setPaidDate(LocalDateTime.now());
                rep.setPaymentId(payment.getId());
                repayments.add(rep);
            } else {
                log.info("Création d'un nouveau repayment pour échéance {}", entry.getInstallmentNumber());
                Repayment repayment = Repayment.builder()
                        .paymentId(payment.getId())
                        .loanId(loanId)
                        .clientId(clientId)
                        .installmentNumber(entry.getInstallmentNumber())
                        .dueAmount(entry.getDueAmount())
                        .paidAmount(entry.getDueAmount())
                        .penaltyAmount(BigDecimal.ZERO)
                        .dueDate(entry.getDueDate())
                        .paidDate(LocalDateTime.now())
                        .status(PaymentStatus.COMPLETED)
                        .build();
                repayments.add(repayment);
            }
        }

        if (!repayments.isEmpty()) {
            repaymentRepository.saveAll(repayments);
            log.info("{} repayments sauvegardés", repayments.size());
        } else {
            log.warn("Aucun repayment à sauvegarder");
        }
    }

    /**
     * Met à jour un schedule local
     */
    private void updateLocalSchedule(String loanId, Integer installmentNumber, String paymentId) {
        Optional<Schedule> localSchedule = scheduleRepository.findByLoanIdAndInstallmentNumber(loanId,
                installmentNumber);
        if (localSchedule.isPresent()) {
            Schedule schedule = localSchedule.get();
            schedule.setPaid(true);
            schedule.setPaidDate(LocalDateTime.now());
            schedule.setPaymentId(paymentId);
            scheduleRepository.save(schedule);
            log.info("   Schedule local mis à jour pour l'échéance {}", installmentNumber);
        } else {
            log.warn("   Schedule local non trouvé pour l'échéance {}", installmentNumber);
        }
    }

    /**
     * Met à jour tous les schedules locaux
     */
    private void updateLocalSchedules(String loanId, List<AmortizationResponse.AmortizationEntry> entries) {
        for (AmortizationResponse.AmortizationEntry entry : entries) {
            updateLocalSchedule(loanId, entry.getInstallmentNumber(), UUID.randomUUID().toString());
        }
    }

    public Map<String, Object> getRepaymentStatus(String loanId, String authorization) {
        log.info("Récupération du statut de remboursement pour loanId: {}", loanId);

        try {
            AmortizationResponse amortization = loanServiceClient.getLoanAmortization(loanId, authorization);

            if (amortization == null || amortization.getEntries() == null) {
                log.warn("Aucune donnée trouvée pour loanId: {}", loanId);
                return Map.of("error", "Aucune donnée trouvée");
            }

            long paidCount = amortization.getEntries().stream()
                    .filter(AmortizationResponse.AmortizationEntry::isPaid)
                    .count();

            boolean fullyPaid = paidCount == amortization.getEntries().size();

            Map<String, Object> response = new HashMap<>();
            response.put("loanId", loanId);
            response.put("loanNumber", amortization.getLoanNumber());
            response.put("totalInstallments", amortization.getEntries().size());
            response.put("paidInstallments", paidCount);
            response.put("remainingInstallments", amortization.getEntries().size() - paidCount);
            response.put("fullyPaid", fullyPaid);
            response.put("totalAmount", amortization.getTotalAmount());
            response.put("totalPaid", amortization.getTotalRepayment().subtract(
                    amortization.getEntries().stream()
                            .filter(entry -> !entry.isPaid())
                            .map(AmortizationResponse.AmortizationEntry::getRemainingBalance)
                            .findFirst().orElse(BigDecimal.ZERO)));
            response.put("remainingBalance", amortization.getEntries().stream()
                    .filter(entry -> !entry.isPaid())
                    .map(AmortizationResponse.AmortizationEntry::getRemainingBalance)
                    .findFirst().orElse(BigDecimal.ZERO));

            log.info("{} échéances restantes sur {}, solde: {} FCFA",
                    response.get("remainingInstallments"),
                    response.get("totalInstallments"),
                    response.get("remainingBalance"));

            if (!fullyPaid) {
                amortization.getEntries().stream()
                        .filter(entry -> !entry.isPaid())
                        .findFirst()
                        .ifPresent(next -> {
                            response.put("nextDueDate", next.getDueDate());
                            response.put("nextDueAmount", next.getDueAmount());
                            log.info("Prochaine échéance: {} FCFA le {}",
                                    next.getDueAmount(), next.getDueDate());
                        });
            }

            return response;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du statut: {}", e.getMessage());
            return Map.of("error", "Impossible de récupérer le statut: " + e.getMessage());
        }
    }

    private String getClientId(String loanId, String authorization) {
        try {
            String clientId = loanServiceClient.getClientIdByLoanId(loanId, authorization);
            log.debug("Client ID récupéré: {} pour loanId: {}", clientId, loanId);
            return clientId;
        } catch (Exception e) {
            log.warn("Impossible de récupérer le clientId: {}", e.getMessage());
            return "unknown";
        }
    }

    private String getClientName(String clientId, String authorization) {
        if (clientId == null || "unknown".equals(clientId)) {
            return null;
        }
        try {
            ClientInfo clientInfo = clientServiceClient.getClientInfo(clientId, authorization);
            if (clientInfo != null) {
                String fullName = clientInfo.getFirstName() + " " + clientInfo.getLastName();
                log.debug("Client name récupéré: {}", fullName);
                return fullName;
            }
            return null;
        } catch (Exception e) {
            log.warn("Impossible de récupérer le clientName: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Récupère les statistiques de remboursements pour une période donnée
     */
    public RepaymentStats getRepaymentStats(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Récupération des statistiques de remboursements du {} au {}", startDate, endDate);

        BigDecimal totalRepayments = repaymentRepository.sumAmountByDateBetween(startDate, endDate);
        Long totalTransactions = repaymentRepository.countByDateBetween(startDate, endDate);
        BigDecimal overdueAmount = repaymentRepository.sumOverdueAmountByDateBetween(startDate, endDate);
        Long overdueCount = repaymentRepository.countOverdueByDateBetween(startDate, endDate);

        Double repaymentRate = calculateRepaymentRate(startDate, endDate);

        return RepaymentStats.builder()
                .totalRepayments(totalRepayments != null ? totalRepayments : BigDecimal.ZERO)
                .totalTransactions(totalTransactions != null ? totalTransactions : 0L)
                .overdueAmount(overdueAmount != null ? overdueAmount : BigDecimal.ZERO)
                .overdueCount(overdueCount != null ? overdueCount : 0L)
                .repaymentRate(repaymentRate != null ? repaymentRate : 0.0)
                .build();
    }

    private Double calculateRepaymentRate(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal totalDue = repaymentRepository.sumDueAmountByDateBetween(startDate, endDate);
        BigDecimal totalPaid = repaymentRepository.sumAmountByDateBetween(startDate, endDate);

        if (totalDue == null || totalDue.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        if (totalPaid == null || totalPaid.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        return totalPaid.divide(totalDue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    // ============================================================
    // STATISTIQUES POUR LISTE DE CLIENTS
    // ============================================================

    public RepaymentStats getRepaymentStatsForClients(List<String> clientIds,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        log.info("Récupération des statistiques de remboursements pour {} clients", clientIds.size());

        if (clientIds == null || clientIds.isEmpty()) {
            log.warn("Liste de clients vide, retour de statistiques vides");
            return createEmptyRepaymentStats();
        }

        try {
            BigDecimal totalRepayments = repaymentRepository.sumAmountByClientIdsAndDateBetween(clientIds, startDate,
                    endDate);
            Long totalTransactions = repaymentRepository.countByClientIdsAndDateBetween(clientIds, startDate, endDate);
            BigDecimal overdueAmount = repaymentRepository.sumOverdueAmountByClientIdsAndDateBetween(clientIds,
                    startDate, endDate);
            Long overdueCount = repaymentRepository.countOverdueByClientIdsAndDateBetween(clientIds, startDate,
                    endDate);

            Double repaymentRate = calculateRepaymentRateForClients(clientIds, startDate, endDate);

            log.info("Stats récupérées: totalRepayments={}, totalTransactions={}, overdueCount={}",
                    totalRepayments, totalTransactions, overdueCount);

            return RepaymentStats.builder()
                    .totalRepayments(totalRepayments != null ? totalRepayments : BigDecimal.ZERO)
                    .totalTransactions(totalTransactions != null ? totalTransactions : 0L)
                    .overdueAmount(overdueAmount != null ? overdueAmount : BigDecimal.ZERO)
                    .overdueCount(overdueCount != null ? overdueCount : 0L)
                    .repaymentRate(repaymentRate != null ? repaymentRate : 0.0)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stats pour clients: {}", e.getMessage(), e);
            return createEmptyRepaymentStats();
        }
    }

    public BigDecimal getTotalRepaymentsForClients(List<String> clientIds,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        log.info("Récupération du total des remboursements pour {} clients", clientIds.size());

        if (clientIds == null || clientIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            BigDecimal total = repaymentRepository.sumAmountByClientIdsAndDateBetween(clientIds, startDate, endDate);
            return total != null ? total : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du total des remboursements: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Double calculateRepaymentRateForClients(List<String> clientIds,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        BigDecimal totalDue = repaymentRepository.sumDueAmountByClientIdsAndDateBetween(clientIds, startDate, endDate);
        BigDecimal totalPaid = repaymentRepository.sumAmountByClientIdsAndDateBetween(clientIds, startDate, endDate);

        if (totalDue == null || totalDue.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        if (totalPaid == null) {
            return 0.0;
        }

        return totalPaid.divide(totalDue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private RepaymentStats createEmptyRepaymentStats() {
        return RepaymentStats.builder()
                .totalRepayments(BigDecimal.ZERO)
                .totalTransactions(0L)
                .overdueAmount(BigDecimal.ZERO)
                .overdueCount(0L)
                .repaymentRate(0.0)
                .build();
    }

    private String mapPaymentMethodToModePaiement(PaymentMethod method) {
        if (method == null)
            return null;
        return switch (method) {
            case CASH -> "ESPECES";
            case BANK_TRANSFER -> "VIREMENT_BANCAIRE";
            case MOBILE_MONEY -> "MOBILE_MONEY";
            case CHECK -> "CHEQUE";
            case CARD -> "MOBILE_MONEY"; // Fallback
            default -> method.name();
        };
    }
}