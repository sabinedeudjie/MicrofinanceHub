package org.example.transactionservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionservice.campay.CamPayException;
import org.example.transactionservice.campay.CamPayService;
import org.example.transactionservice.campay.dto.CamPayCollectResponse;
import org.example.transactionservice.campay.dto.CamPayDisburseResponse;
import org.example.transactionservice.campay.dto.CamPayWebhookPayload;
import org.example.transactionservice.client.CompteServiceClient;
import org.example.transactionservice.config.RabbitMQConfig;
import org.example.transactionservice.dto.*;
import org.example.transactionservice.event.MobileMoneyRemboursementConfirmeEvent;
import org.example.transactionservice.event.SoldeInsuffisantEvent;
import org.example.transactionservice.event.TransactionEffectueeEvent;
import org.example.transactionservice.exception.OperationInvalideException;
import org.example.transactionservice.model.*;
import org.example.transactionservice.repository.TransactionRepository;
import org.example.transactionservice.repository.TransactionSpecification;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CompteServiceClient compteServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final CamPayService camPayService;

    @Value("${app.transaction.montant-minimum:1}")
    private BigDecimal montantMinimum;

    // ============================================================
    // DÉPÔT
    // ============================================================

    public TransactionResponse effectuerDepot(Long compteId, DepotRequest request) {
        CompteInfo compte = compteServiceClient.getCompteById(compteId);

        if (request.getMontant().compareTo(montantMinimum) < 0) {
            throw new OperationInvalideException("MONTANT_MINIMUM", "Montant minimum : " + montantMinimum + " XAF");
        }
        if (!compte.isOperationnel()) {
            throw new OperationInvalideException(
                    "Le compte " + compte.getNumeroCompte() + " n'est pas actif (statut : " + compte.getStatut() + ")");
        }
        if (!compte.respectePlafond(request.getMontant())) {
            throw new OperationInvalideException(
                    "Ce dépôt dépasserait le plafond autorisé de " + compte.getPlafond() + " XAF");
        }

        if (ModePaiement.MOBILE_MONEY.equals(request.getModeDepot())) {
            return effectuerDepotMobileMoney(compte, request);
        }
        return effectuerDepotClassique(compte, request);
    }

    private TransactionResponse effectuerDepotClassique(CompteInfo compte, DepotRequest request) {
        BigDecimal soldeAvant = compte.getSolde();
        BigDecimal soldeApres = soldeAvant.add(request.getMontant());

        compteServiceClient.crediterCompte(compte.getId(), request.getMontant());

        Transaction transaction = enregistrerTransaction(
                compte.getId(), TypeTransaction.DEPOT,
                request.getMontant(), soldeAvant, soldeApres,
                request.getDescription(), request.getModeDepot());

        log.info("Dépôt classique de {} XAF sur le compte {} (réf: {})",
                request.getMontant(), compte.getNumeroCompte(), transaction.getReference());

        publierEvenementTransaction(compte, transaction, RabbitMQConfig.RK_TRANSACTION_DEPOT);
        return mapToResponse(transaction);
    }

    private TransactionResponse effectuerDepotMobileMoney(CompteInfo compte, DepotRequest request) {
        if (request.getNumeroPaiement() == null || request.getNumeroPaiement().isBlank()) {
            throw new OperationInvalideException(
                    "Le numéro de téléphone Mobile Money est obligatoire pour un dépôt MOBILE_MONEY");
        }

        String referenceInterne = genererReference();
        String description = request.getDescription() != null
                ? request.getDescription()
                : "Dépôt Mobile Money - MicroFinanceHub";

        Transaction transaction = Transaction.builder()
                .compteId(compte.getId())
                .typeTransaction(TypeTransaction.DEPOT)
                .montant(request.getMontant())
                .soldeAvant(compte.getSolde())
                .soldeApres(compte.getSolde())
                .dateTransaction(LocalDateTime.now())
                .reference(referenceInterne)
                .statut(StatutTransaction.EN_TRAITEMENT)
                .description(description)
                .modePaiement(ModePaiement.MOBILE_MONEY)
                .numeroPaiement(request.getNumeroPaiement())
                .build();
        transaction = transactionRepository.save(transaction);

        try {
            CamPayCollectResponse campayResponse = camPayService.initierCollecte(
                    request.getNumeroPaiement(), request.getMontant(), referenceInterne, description);
            transaction.setCampayReference(campayResponse.getReference());
            transactionRepository.save(transaction);

            log.info("Dépôt Mobile Money initié : {} XAF, compte {}, réf CamPay {}",
                    request.getMontant(), compte.getNumeroCompte(), campayResponse.getReference());

        } catch (CamPayException e) {
            transaction.setStatut(StatutTransaction.ECHOUEE);
            transaction.setDescription(description + " [ÉCHEC: " + e.getMessage() + "]");
            transactionRepository.save(transaction);
            log.error("Échec initiation CamPay pour le compte {} : {}", compte.getNumeroCompte(), e.getMessage());

            String code = e.getMessage().contains("ER201") ? "DEMO_LIMIT" : "MOBILE_MONEY_INIT_FAILED";
            throw new OperationInvalideException(code,
                    "Échec de l'initiation du paiement Mobile Money : " + e.getMessage());
        }

        return mapToResponse(transaction);
    }

    // ============================================================
    // RETRAIT
    // ============================================================

    public TransactionResponse effectuerRetrait(Long compteId, RetraitRequest request) {
        CompteInfo compte = compteServiceClient.getCompteById(compteId);

        if (request.getMontant().compareTo(montantMinimum) < 0) {
            throw new OperationInvalideException("MONTANT_MINIMUM", "Montant minimum : " + montantMinimum + " XAF");
        }
        if (!compte.isOperationnel()) {
            throw new OperationInvalideException(
                    "Le compte " + compte.getNumeroCompte() + " n'est pas actif");
        }
        if (!compte.hasSoldeSuffisant(request.getMontant())) {
            publierEvenement(RabbitMQConfig.RK_ALERTE_SOLDE,
                    SoldeInsuffisantEvent.builder()
                            .compteId(compte.getId())
                            .clientId(compte.getClientId())
                            .clientNom(compte.getClientNom())
                            .clientEmail(compte.getClientEmail())
                            .numeroCompte(compte.getNumeroCompte())
                            .soldeActuel(compte.getSolde())
                            .montantDemande(request.getMontant())
                            .timestamp(LocalDateTime.now())
                            .build());
            throw new OperationInvalideException("SOLDE_INSUFFISANT",
                    "Solde insuffisant. Solde actuel : " + compte.getSolde() + " XAF, " +
                            "montant demandé : " + request.getMontant() + " XAF");
        }

        if (ModePaiement.MOBILE_MONEY.equals(request.getModeRetrait())) {
            return effectuerRetraitMobileMoney(compte, request);
        }
        return effectuerRetraitClassique(compte, request);
    }

    private TransactionResponse effectuerRetraitClassique(CompteInfo compte, RetraitRequest request) {
        BigDecimal soldeAvant = compte.getSolde();
        BigDecimal soldeApres = soldeAvant.subtract(request.getMontant());

        compteServiceClient.debiterCompte(compte.getId(), request.getMontant());

        Transaction transaction = enregistrerTransaction(
                compte.getId(), TypeTransaction.RETRAIT,
                request.getMontant(), soldeAvant, soldeApres,
                request.getDescription(), request.getModeRetrait());

        log.info("Retrait classique de {} XAF sur le compte {} (réf: {})",
                request.getMontant(), compte.getNumeroCompte(), transaction.getReference());

        publierEvenementTransaction(compte, transaction, RabbitMQConfig.RK_TRANSACTION_RETRAIT);
        return mapToResponse(transaction);
    }

    private TransactionResponse effectuerRetraitMobileMoney(CompteInfo compte, RetraitRequest request) {
        if (request.getNumeroPaiement() == null || request.getNumeroPaiement().isBlank()) {
            throw new OperationInvalideException(
                    "Le numéro de téléphone Mobile Money du bénéficiaire est obligatoire pour un retrait MOBILE_MONEY");
        }

        BigDecimal soldeAvant = compte.getSolde();
        BigDecimal soldeApres = soldeAvant.subtract(request.getMontant());
        String referenceInterne = genererReference();
        String description = request.getDescription() != null
                ? request.getDescription()
                : "Retrait Mobile Money - MicroFinanceHub";

        compteServiceClient.debiterCompte(compte.getId(), request.getMontant());

        Transaction transaction = Transaction.builder()
                .compteId(compte.getId())
                .typeTransaction(TypeTransaction.RETRAIT)
                .montant(request.getMontant())
                .soldeAvant(soldeAvant)
                .soldeApres(soldeApres)
                .dateTransaction(LocalDateTime.now())
                .reference(referenceInterne)
                .statut(StatutTransaction.EN_TRAITEMENT)
                .description(description)
                .modePaiement(ModePaiement.MOBILE_MONEY)
                .numeroPaiement(request.getNumeroPaiement())
                .build();
        transaction = transactionRepository.save(transaction);

        try {
            CamPayDisburseResponse campayResponse = camPayService.effectuerDecaissement(
                    request.getNumeroPaiement(), request.getMontant(), referenceInterne, description);
            transaction.setCampayReference(campayResponse.getReference());

            if ("SUCCESSFUL".equalsIgnoreCase(campayResponse.getStatus())) {
                transaction.setStatut(StatutTransaction.COMPLETEE);
            }
            transactionRepository.save(transaction);

        } catch (CamPayException e) {
            log.error("Échec CamPay décaissement, annulation du débit compte {} : {}",
                    compte.getNumeroCompte(), e.getMessage());
            compteServiceClient.crediterCompte(compte.getId(), request.getMontant());

            transaction.setStatut(StatutTransaction.ECHOUEE);
            transaction.setSoldeApres(soldeAvant);
            transaction.setDescription(description + " [ANNULÉ: " + e.getMessage() + "]");
            transactionRepository.save(transaction);

            throw new OperationInvalideException("Échec du décaissement Mobile Money : " + e.getMessage());
        }

        if (StatutTransaction.COMPLETEE.equals(transaction.getStatut())) {
            publierEvenementTransaction(compte, transaction, RabbitMQConfig.RK_TRANSACTION_RETRAIT);
        }

        return mapToResponse(transaction);
    }

    // ============================================================
    // VIREMENT
    // ============================================================

    @Transactional
    public TransactionResponse effectuerVirement(Long compteSourceId, VirementRequest request) {
        CompteInfo compteSource = compteServiceClient.getCompteById(compteSourceId);
        CompteInfo compteDest = compteServiceClient.getCompteByNumero(request.getNumeroCompteDestination());

        if (!compteSource.isOperationnel()) {
            throw new OperationInvalideException("Le compte source n'est pas actif");
        }
        if (!compteDest.isOperationnel()) {
            throw new OperationInvalideException("Le compte destinataire n'est pas actif");
        }
        if (compteSource.getId().equals(compteDest.getId())) {
            throw new OperationInvalideException("Impossible de virer vers le même compte");
        }
        if (!compteSource.hasSoldeSuffisant(request.getMontant())) {
            publierEvenement(RabbitMQConfig.RK_ALERTE_SOLDE,
                    SoldeInsuffisantEvent.builder()
                            .compteId(compteSource.getId())
                            .clientId(compteSource.getClientId())
                            .clientNom(compteSource.getClientNom())
                            .clientEmail(compteSource.getClientEmail())
                            .numeroCompte(compteSource.getNumeroCompte())
                            .soldeActuel(compteSource.getSolde())
                            .montantDemande(request.getMontant())
                            .timestamp(LocalDateTime.now())
                            .build());
            throw new OperationInvalideException("SOLDE_INSUFFISANT", "Solde insuffisant pour effectuer ce virement");
        }

        BigDecimal soldeAvantSource = compteSource.getSolde();
        BigDecimal soldeApresSource = soldeAvantSource.subtract(request.getMontant());
        BigDecimal soldeAvantDest = compteDest.getSolde();
        BigDecimal soldeApresDest = soldeAvantDest.add(request.getMontant());

        compteServiceClient.debiterCompte(compteSource.getId(), request.getMontant());
        compteServiceClient.crediterCompte(compteDest.getId(), request.getMontant());

        Transaction txSortante = enregistrerTransaction(
                compteSource.getId(), TypeTransaction.VIREMENT_SORTANT,
                request.getMontant(), soldeAvantSource, soldeApresSource,
                "Virement vers " + compteDest.getNumeroCompte() + " - " + request.getMotif(),
                ModePaiement.VIREMENT_INTERNE);
        txSortante.setCompteDestination(compteDest.getNumeroCompte());
        transactionRepository.save(txSortante);

        Transaction txEntrante = enregistrerTransaction(
                compteDest.getId(), TypeTransaction.VIREMENT_ENTRANT,
                request.getMontant(), soldeAvantDest, soldeApresDest,
                "Virement de " + compteSource.getNumeroCompte() + " - " + request.getMotif(),
                ModePaiement.VIREMENT_INTERNE);

        log.info("Virement de {} XAF : {} vers {} (ref: {})",
                request.getMontant(), compteSource.getNumeroCompte(),
                compteDest.getNumeroCompte(), txSortante.getReference());

        publierEvenementVirement(compteSource, compteDest, txSortante, RabbitMQConfig.RK_TRANSACTION_VIREMENT);
        publierEvenementVirement(compteSource, compteDest, txEntrante, RabbitMQConfig.RK_TRANSACTION_VIREMENT);
        return mapToResponse(txSortante);
    }

    // ============================================================
    // WEBHOOKS CAMPAY
    // ============================================================

    @Transactional
    public void traiterWebhookDepot(CamPayWebhookPayload payload) {
        log.info("Webhook CamPay dépôt : réf={}, statut={}", payload.getReference(), payload.getStatus());

        Transaction transaction = trouverTransactionWebhook(payload);
        if (transaction == null)
            return;

        if (!StatutTransaction.EN_TRAITEMENT.equals(transaction.getStatut())) {
            log.info("Webhook ignoré : transaction {} déjà en statut {}", transaction.getReference(),
                    transaction.getStatut());
            return;
        }

        if (payload.isSuccessful()) {
            compteServiceClient.crediterCompte(transaction.getCompteId(), transaction.getMontant());

            CompteInfo compte = compteServiceClient.getCompteById(transaction.getCompteId());
            transaction.setStatut(StatutTransaction.COMPLETEE);
            transaction.setSoldeApres(compte.getSolde());
            transactionRepository.save(transaction);

            log.info("Dépôt Mobile Money confirmé : {} XAF crédités sur le compte {}",
                    transaction.getMontant(), transaction.getCompteId());

            publierEvenementTransaction(compte, transaction, RabbitMQConfig.RK_TRANSACTION_DEPOT);

        } else if (payload.isFailed()) {
            transaction.setStatut(StatutTransaction.ECHOUEE);
            transactionRepository.save(transaction);
        }
    }

    @Transactional
    public void traiterWebhookRetrait(CamPayWebhookPayload payload) {
        log.info("Webhook CamPay retrait : réf={}, statut={}", payload.getReference(), payload.getStatus());

        Transaction transaction = trouverTransactionWebhook(payload);
        if (transaction == null)
            return;

        if (!StatutTransaction.EN_TRAITEMENT.equals(transaction.getStatut())) {
            log.info("Webhook ignoré : transaction {} déjà en statut {}", transaction.getReference(),
                    transaction.getStatut());
            return;
        }

        if (payload.isSuccessful()) {
            transaction.setStatut(StatutTransaction.COMPLETEE);
            transactionRepository.save(transaction);

            CompteInfo compte = compteServiceClient.getCompteById(transaction.getCompteId());
            publierEvenementTransaction(compte, transaction, RabbitMQConfig.RK_TRANSACTION_RETRAIT);

        } else if (payload.isFailed()) {
            compteServiceClient.crediterCompte(transaction.getCompteId(), transaction.getMontant());

            CompteInfo compte = compteServiceClient.getCompteById(transaction.getCompteId());
            transaction.setStatut(StatutTransaction.ECHOUEE);
            transaction.setSoldeApres(compte.getSolde());
            transactionRepository.save(transaction);

            log.warn("Retrait Mobile Money échoué — solde restauré (réf CamPay: {})", payload.getReference());
        }
    }

    // ============================================================
    // CONSULTATION
    // ============================================================

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getHistorique(Long compteId, Pageable pageable) {
        return transactionRepository.findByCompteIdOrderByDateTransactionDesc(compteId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getByPeriode(Long compteId, LocalDateTime debut, LocalDateTime fin,
            Pageable pageable) {
        return transactionRepository.findByCompteIdAndPeriode(compteId, debut, fin, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getByType(Long compteId, TypeTransaction type, Pageable pageable) {
        return transactionRepository.findByCompteIdAndTypeTransaction(compteId, type, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getByStatut(StatutTransaction statut, Pageable pageable) {
        return transactionRepository.findByStatut(statut, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public long compterTransactions(Long compteId) {
        return transactionRepository.countByCompteId(compteId);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> rechercher(
            Long compteId, String reference, TypeTransaction typeTransaction,
            StatutTransaction statut, BigDecimal montantMin, BigDecimal montantMax,
            LocalDateTime debut, LocalDateTime fin, Pageable pageable) {
        return transactionRepository.findAll(
                TransactionSpecification.withCriteria(
                        compteId, reference, typeTransaction, statut, montantMin, montantMax, debut, fin),
                pageable).map(this::mapToResponse);
    }

    @Transactional
    public int relancerTransactionsEnAttente() {
        List<Transaction> enAttente = transactionRepository.findByStatut(StatutTransaction.EN_TRAITEMENT);
        int traitees = 0;

        for (Transaction transaction : enAttente) {
            if (transaction.getCampayReference() == null)
                continue;

            try {
                var statut = camPayService.verifierStatut(transaction.getCampayReference());

                if (statut.isSuccessful()) {
                    CompteInfo compte = null;
                    if (TypeTransaction.DEPOT.equals(transaction.getTypeTransaction())) {
                        compteServiceClient.crediterCompte(transaction.getCompteId(), transaction.getMontant());
                        compte = compteServiceClient.getCompteById(transaction.getCompteId());
                        transaction.setSoldeApres(compte.getSolde());
                    }
                    transaction.setStatut(StatutTransaction.COMPLETEE);
                    transactionRepository.save(transaction);
                    if (compte != null) {
                        publierEvenementTransaction(compte, transaction, RabbitMQConfig.RK_TRANSACTION_DEPOT);
                    }
                    traitees++;

                } else if (statut.isFailed()) {
                    CompteInfo compte = null;
                    if (TypeTransaction.RETRAIT.equals(transaction.getTypeTransaction())) {
                        compteServiceClient.crediterCompte(transaction.getCompteId(), transaction.getMontant());
                        compte = compteServiceClient.getCompteById(transaction.getCompteId());
                        transaction.setSoldeApres(compte.getSolde());
                    }
                    transaction.setStatut(StatutTransaction.ECHOUEE);
                    transactionRepository.save(transaction);
                    if (compte != null) {
                        publierEvenementTransaction(compte, transaction, RabbitMQConfig.RK_TRANSACTION_RETRAIT);
                    }
                    traitees++;
                }

            } catch (Exception e) {
                log.error("Echec de verification de la transaction {} via CamPay : {}",
                        transaction.getReference(), e.getMessage());
            }
        }

        log.info("Relance terminée : {}/{} transactions traitées", traitees, enAttente.size());
        return traitees;
    }

    // ============================================================
    // REMBOURSEMENT PRÊT
    // ============================================================

    @Transactional
    public TransactionResponse effectuerRemboursementPret(RemboursementPretRequest request) {
        String referenceInterne = genererReference();
        String ref = (request.getReferenceRepayment() != null && !request.getReferenceRepayment().isBlank())
                ? request.getReferenceRepayment()
                : referenceInterne;
        String description = request.getDescription() != null
                ? request.getDescription()
                : "Remboursement prêt " + request.getLoanId();

        if (ModePaiement.MOBILE_MONEY.equals(request.getModePaiement())) {
            if (request.getNumeroPaiement() == null || request.getNumeroPaiement().isBlank()) {
                throw new OperationInvalideException(
                        "Le numéro Mobile Money est obligatoire pour ce mode de paiement");
            }

            Transaction transaction = Transaction.builder()
                    .compteId(0L)
                    .loanId(request.getLoanId())
                    .clientId(request.getClientId())
                    .typeTransaction(TypeTransaction.REMBOURSEMENT_PRET)
                    .montant(request.getMontant())
                    .soldeAvant(BigDecimal.ZERO)
                    .soldeApres(BigDecimal.ZERO)
                    .dateTransaction(LocalDateTime.now())
                    .reference(ref)
                    .statut(StatutTransaction.EN_TRAITEMENT)
                    .description(description)
                    .modePaiement(ModePaiement.MOBILE_MONEY)
                    .numeroPaiement(request.getNumeroPaiement())
                    .build();
            transaction = transactionRepository.save(transaction);

            try {
                CamPayCollectResponse campayResponse = camPayService.initierCollecte(
                        request.getNumeroPaiement(), request.getMontant(), referenceInterne, description);
                transaction.setCampayReference(campayResponse.getReference());
                transactionRepository.save(transaction);
                log.info("Remboursement Mobile Money initié: loanId={}, réf CamPay={}",
                        request.getLoanId(), campayResponse.getReference());
            } catch (CamPayException e) {
                transaction.setStatut(StatutTransaction.ECHOUEE);
                transactionRepository.save(transaction);
                String code = e.getMessage().contains("ER201") ? "DEMO_LIMIT" : "MOBILE_MONEY_INIT_FAILED";
                throw new OperationInvalideException(code, "Échec initiation Mobile Money: " + e.getMessage());
            }

            return mapToResponse(transaction);

        } else if (ModePaiement.VIREMENT_BANCAIRE.equals(request.getModePaiement())) {
            // VIREMENT BANCAIRE — débiter le compte du client
            if (request.getCompteSourceId() == null) {
                throw new OperationInvalideException("Le compte source est obligatoire pour un virement bancaire");
            }

            CompteInfo compte = compteServiceClient.getCompteById(request.getCompteSourceId());
            if (!compte.hasSoldeSuffisant(request.getMontant())) {
                throw new OperationInvalideException("SOLDE_INSUFFISANT",
                        "Solde insuffisant sur le compte " + compte.getNumeroCompte());
            }

            BigDecimal soldeAvant = compte.getSolde();
            BigDecimal soldeApres = soldeAvant.subtract(request.getMontant());

            compteServiceClient.debiterCompte(compte.getId(), request.getMontant());

            Transaction transaction = Transaction.builder()
                    .compteId(compte.getId())
                    .loanId(request.getLoanId())
                    .clientId(request.getClientId())
                    .typeTransaction(TypeTransaction.REMBOURSEMENT_PRET)
                    .montant(request.getMontant())
                    .soldeAvant(soldeAvant)
                    .soldeApres(soldeApres)
                    .dateTransaction(LocalDateTime.now())
                    .reference(ref)
                    .statut(StatutTransaction.COMPLETEE)
                    .description(description + " (Débit compte " + compte.getNumeroCompte() + ")")
                    .modePaiement(ModePaiement.VIREMENT_BANCAIRE)
                    .build();
            transaction = transactionRepository.save(transaction);

            log.info("Remboursement par virement effectué: loanId={}, montant={}, compte={}",
                    request.getLoanId(), request.getMontant(), compte.getNumeroCompte());

            // On peut aussi publier un événement si besoin
            return mapToResponse(transaction);

        } else {
            // ESPECES, CHEQUE — enregistrement immédiat (cash déjà reçu par l'agent)
            Transaction transaction = Transaction.builder()
                    .compteId(0L)
                    .loanId(request.getLoanId())
                    .clientId(request.getClientId())
                    .typeTransaction(TypeTransaction.REMBOURSEMENT_PRET)
                    .montant(request.getMontant())
                    .soldeAvant(BigDecimal.ZERO)
                    .soldeApres(BigDecimal.ZERO)
                    .dateTransaction(LocalDateTime.now())
                    .reference(ref)
                    .statut(StatutTransaction.COMPLETEE)
                    .description(description)
                    .modePaiement(request.getModePaiement())
                    .numeroPaiement(request.getNumeroPaiement())
                    .build();
            transaction = transactionRepository.save(transaction);
            log.info("Remboursement {} enregistré: loanId={}, montant={}",
                    request.getModePaiement(), request.getLoanId(), request.getMontant());
            return mapToResponse(transaction);
        }
    }

    @Transactional
    public void traiterWebhookRemboursement(CamPayWebhookPayload payload) {
        log.info("Webhook CamPay remboursement: réf={}, statut={}", payload.getReference(), payload.getStatus());

        Transaction transaction = trouverTransactionWebhook(payload);
        if (transaction == null) {
            log.warn("Webhook remboursement: aucune transaction trouvée (réf={})", payload.getReference());
            return;
        }

        if (!StatutTransaction.EN_TRAITEMENT.equals(transaction.getStatut())) {
            log.info("Webhook ignoré: transaction {} déjà en statut {}", transaction.getReference(),
                    transaction.getStatut());
            return;
        }

        if (payload.isSuccessful()) {
            transaction.setStatut(StatutTransaction.COMPLETEE);
            transactionRepository.save(transaction);
            log.info("Remboursement Mobile Money confirmé: loanId={}, montant={} XAF",
                    transaction.getLoanId(), transaction.getMontant());

            MobileMoneyRemboursementConfirmeEvent event = MobileMoneyRemboursementConfirmeEvent.builder()
                    .loanId(transaction.getLoanId())
                    .clientId(transaction.getClientId())
                    .clientEmail((compteServiceClient.getClientCompteInfo(transaction.getClientId()) != null)
                            ? compteServiceClient.getClientCompteInfo(transaction.getClientId()).getClientEmail()
                            : null)
                    .clientNom((compteServiceClient.getClientCompteInfo(transaction.getClientId()) != null)
                            ? compteServiceClient.getClientCompteInfo(transaction.getClientId()).getClientNom()
                            : null)
                    .montant(transaction.getMontant())
                    .referenceRepayment(transaction.getReference())
                    .campayReference(transaction.getCampayReference())
                    .timestamp(LocalDateTime.now())
                    .build();
            publierEvenement(RabbitMQConfig.REPAYMENT_EXCHANGE, RabbitMQConfig.RK_REPAYMENT_MOBILE_CONFIRMED, event);

        } else if (payload.isFailed()) {
            transaction.setStatut(StatutTransaction.ECHOUEE);
            transactionRepository.save(transaction);
            log.warn("Remboursement Mobile Money échoué: loanId={}, réf CamPay={}",
                    transaction.getLoanId(), payload.getReference());

            MobileMoneyRemboursementConfirmeEvent event = MobileMoneyRemboursementConfirmeEvent.builder()
                    .loanId(transaction.getLoanId())
                    .clientId(transaction.getClientId())
                    .clientEmail((compteServiceClient.getClientCompteInfo(transaction.getClientId()) != null)
                            ? compteServiceClient.getClientCompteInfo(transaction.getClientId()).getClientEmail()
                            : null)
                    .clientNom((compteServiceClient.getClientCompteInfo(transaction.getClientId()) != null)
                            ? compteServiceClient.getClientCompteInfo(transaction.getClientId()).getClientNom()
                            : null)
                    .montant(transaction.getMontant())
                    .referenceRepayment(transaction.getReference())
                    .campayReference(transaction.getCampayReference())
                    .timestamp(LocalDateTime.now())
                    .build();
            publierEvenement(RabbitMQConfig.REPAYMENT_EXCHANGE, RabbitMQConfig.RK_REPAYMENT_MOBILE_FAILED, event);
        }
    }

    // ============================================================
    // MÉTHODES PRIVÉES
    // ============================================================

    private Transaction trouverTransactionWebhook(CamPayWebhookPayload payload) {
        return transactionRepository.findByCampayReference(payload.getReference())
                .orElseGet(() -> {
                    if (payload.getExternalReference() != null) {
                        return transactionRepository.findByReference(payload.getExternalReference()).orElse(null);
                    }
                    return null;
                });
    }

    private String genererReference() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String reference;
        do {
            long seq = System.nanoTime() % 1_000_000;
            reference = String.format("TXN-%s-%06d", date, seq);
        } while (transactionRepository.existsByReference(reference));
        return reference;
    }

    private Transaction enregistrerTransaction(
            Long compteId, TypeTransaction type, BigDecimal montant,
            BigDecimal soldeAvant, BigDecimal soldeApres,
            String description, ModePaiement modePaiement) {

        Transaction transaction = Transaction.builder()
                .compteId(compteId)
                .typeTransaction(type)
                .montant(montant)
                .soldeAvant(soldeAvant)
                .soldeApres(soldeApres)
                .dateTransaction(LocalDateTime.now())
                .reference(genererReference())
                .statut(StatutTransaction.COMPLETEE)
                .description(description)
                .modePaiement(modePaiement)
                .build();

        return transactionRepository.save(transaction);
    }

    private void publierEvenementTransaction(CompteInfo compte, Transaction tx, String routingKey) {
        EventType evType = EventType.valueOf(tx.getTypeTransaction().name());
        publierEvenement(routingKey, TransactionEffectueeEvent.builder()
                .eventType(evType)
                .typeEvent(evType.name())
                .transactionId(tx.getId())
                .compteId(compte.getId())
                .clientId(compte.getClientId())
                .clientEmail(compte.getClientEmail())
                .clientNom(compte.getClientNom())
                .numeroCompte(compte.getNumeroCompte())
                .typeCompte(compte.getTypeCompte())
                .compteContrepartie(tx.getCompteDestination())
                .montant(tx.getMontant())
                .soldeApres(tx.getSoldeApres())
                .reference(tx.getReference())
                .description(tx.getDescription())
                .dateTransaction(tx.getDateTransaction())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private void publierEvenementVirement(CompteInfo compteExpediteur, CompteInfo compteDestinataire,
            Transaction tx, String routingKey) {
        EventType evType = EventType.valueOf(tx.getTypeTransaction().name());
        boolean estEntrant = evType == EventType.VIREMENT_ENTRANT;
        CompteInfo compteConcerne = estEntrant ? compteDestinataire : compteExpediteur;
        CompteInfo compteContrepartie = estEntrant ? compteExpediteur : compteDestinataire;
        publierEvenement(routingKey, TransactionEffectueeEvent.builder()
                .eventType(evType)
                .typeEvent(evType.name())
                .transactionId(tx.getId())
                .compteId(compteConcerne.getId())
                .clientId(compteConcerne.getClientId())
                .clientEmail(compteConcerne.getClientEmail())
                .clientNom(compteConcerne.getClientNom())
                .numeroCompte(compteConcerne.getNumeroCompte())
                .typeCompte(compteConcerne.getTypeCompte())
                .compteContrepartie(compteContrepartie.getNumeroCompte())
                .nomContrepartie(compteContrepartie.getClientNom())
                .montant(tx.getMontant())
                .soldeApres(tx.getSoldeApres())
                .reference(tx.getReference())
                .description(tx.getDescription())
                .dateTransaction(tx.getDateTransaction())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private void publierEvenement(String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.COMPTE_EXCHANGE, routingKey, event);
        } catch (Exception ex) {
            log.error("Échec publication RabbitMQ (routingKey: {}) : {}", routingKey, ex.getMessage());
        }
    }

    private void publierEvenement(String exchange, String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
        } catch (Exception ex) {
            log.error("Échec publication RabbitMQ (exchange: {}, routingKey: {}) : {}",
                    exchange, routingKey, ex.getMessage());
        }
    }

    public TransactionResponse mapToResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .compteId(tx.getCompteId())
                .typeTransaction(tx.getTypeTransaction())
                .montant(tx.getMontant())
                .soldeAvant(tx.getSoldeAvant())
                .soldeApres(tx.getSoldeApres())
                .dateTransaction(tx.getDateTransaction())
                .reference(tx.getReference())
                .statut(tx.getStatut())
                .description(tx.getDescription())
                .modePaiement(tx.getModePaiement())
                .numeroPaiement(tx.getNumeroPaiement())
                .campayReference(tx.getCampayReference())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}