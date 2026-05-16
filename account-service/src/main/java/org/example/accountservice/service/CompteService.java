package org.example.accountservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.accountservice.config.RabbitMQConfig;
import org.example.accountservice.dto.*;
import org.example.accountservice.event.CompteOuvertEvent;
import org.example.accountservice.exception.CompteNotFoundException;
import org.example.accountservice.exception.OperationInvalideException;
import org.example.accountservice.model.*;
import org.example.accountservice.repository.CompteRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CompteService {

    private final CompteRepository compteRepository;
    private final RabbitTemplate rabbitTemplate;

    private final AtomicLong refCounter = new AtomicLong(0);

    public CompteResponse ouvrirCompte(OuvrirCompteRequest request) {
        log.info("d'un compte {} pour le client {}", request.getTypeCompte(), request.getClientId());

        // Un client ne peut pas avoir deux comptes du même type en cours (hors comptes fermés/rejetés)
        List<StatutCompte> statutsActifs = List.of(
            StatutCompte.EN_ATTENTE_VALIDATION,
            StatutCompte.ACTIF,
            StatutCompte.BLOQUE,
            StatutCompte.SUSPENDU,
            StatutCompte.INACTIF
        );
        if (compteRepository.existsByClientIdAndTypeCompteAndStatutIn(
                request.getClientId(), request.getTypeCompte(), statutsActifs)) {
            String typeLabel = switch (request.getTypeCompte()) {
                case EPARGNE       -> "Épargne";
                case COURANT       -> "Courant";
                case DEPOT_A_TERME -> "Dépôt à Terme";
                case MICRO_EPARGNE -> "Micro-Épargne";
                case CREDIT        -> "Crédit";
            };
            throw new OperationInvalideException(
                "Ce client a déjà un compte " + typeLabel + ". Un seul compte par type est autorisé.");
        }

        String numeroCompte = genererNumeroCompte();
        BigDecimal tauxInteret = getTauxParDefaut(request.getTypeCompte());

        Compte compte = Compte.builder()
                .clientId(request.getClientId())
                .numeroCompte(numeroCompte)
                .typeCompte(request.getTypeCompte())
                .solde(request.getSoldeInitial() != null ? request.getSoldeInitial() : BigDecimal.ZERO)
                .devise(Devise.XAF)
                .statut(StatutCompte.EN_ATTENTE_VALIDATION)
                .dateOuverture(LocalDateTime.now())
                .tauxInteret(tauxInteret)
                .soldeMinimum(BigDecimal.ZERO)
                .description(request.getDescription())
                .clientEmail(request.getClientEmail())
                .clientNom(request.getClientNom())
                .build();

        Compte savedCompte = compteRepository.save(compte);
        log.info("créé avec succès : {}", numeroCompte);

        publierEvenement(
                RabbitMQConfig.RK_COMPTE_OUVERT,
                CompteOuvertEvent.builder()
                        .compteId(savedCompte.getId())
                        .clientId(savedCompte.getClientId())
                        .clientEmail(request.getClientEmail())
                        .clientNom(request.getClientNom())
                        .numeroCompte(numeroCompte)
                        .typeCompte(savedCompte.getTypeCompte().name())
                        .dateOuverture(savedCompte.getDateOuverture())
                        .timestamp(LocalDateTime.now())
                        .build()
        );

        return mapToCompteResponse(savedCompte);
    }

    @Transactional(readOnly = true)
    public CompteResponse getCompteById(Long id) {
        return mapToCompteResponse(findCompteOrThrow(id));
    }

    @Transactional(readOnly = true)
    public CompteResponse getCompteByNumero(String numeroCompte) {
        Compte compte = compteRepository.findByNumeroCompte(numeroCompte)
                .orElseThrow(() -> new CompteNotFoundException(numeroCompte));
        return mapToCompteResponse(compte);
    }

    @Transactional(readOnly = true)
    public Page<CompteResponse> getComptesByClientId(String clientId, Pageable pageable) {
        return compteRepository.findByClientId(clientId, pageable).map(this::mapToCompteResponse);
    }

    @Transactional(readOnly = true)
    public BigDecimal consulterSolde(Long compteId) {
        return findCompteOrThrow(compteId).getSolde();
    }

    public CompteResponse modifierCompte(Long compteId, ModifierCompteRequest request) {
        Compte compte = findCompteOrThrow(compteId);

        if (request.getDescription() != null)  compte.setDescription(request.getDescription());
        if (request.getSoldeMinimum() != null)  compte.setSoldeMinimum(request.getSoldeMinimum());
        if (request.getPlafond() != null)        compte.setPlafond(request.getPlafond());
        if (request.getTauxInteret() != null)    compte.setTauxInteret(request.getTauxInteret());
        compte.setDateModification(LocalDateTime.now());

        return mapToCompteResponse(compteRepository.save(compte));
    }

    public void supprimerCompte(Long compteId) {
        Compte compte = findCompteOrThrow(compteId);

        if (compte.getStatut() != StatutCompte.FERME) {
            throw new OperationInvalideException(
                    "Le compte doit être fermé avant d'être supprimé (statut : " + compte.getStatut() + ")");
        }
        if (compte.getSolde().compareTo(BigDecimal.ZERO) != 0) {
            throw new OperationInvalideException(
                    "Le compte ne peut pas être supprimé : solde non nul (" + compte.getSolde() + " XAF)");
        }

        compteRepository.delete(compte);
    }

    public CompteResponse changerStatut(Long compteId, StatutCompte nouveauStatut) {
        Compte compte = findCompteOrThrow(compteId);
        compte.setStatut(nouveauStatut);
        compte.setDateModification(LocalDateTime.now());
        Compte saved = compteRepository.save(compte);

        if (nouveauStatut == StatutCompte.ACTIF) {
            publierEvenement(
                RabbitMQConfig.RK_COMPTE_VALIDE,
                CompteOuvertEvent.builder()
                    .typeEvent("VALIDE")
                    .compteId(saved.getId())
                    .clientId(saved.getClientId())
                    .clientEmail(saved.getClientEmail())
                    .clientNom(saved.getClientNom())
                    .numeroCompte(saved.getNumeroCompte())
                    .typeCompte(saved.getTypeCompte().name())
                    .dateOuverture(saved.getDateOuverture())
                    .timestamp(LocalDateTime.now())
                    .build()
            );
        } else if (nouveauStatut == StatutCompte.REJETE) {
            publierEvenement(
                RabbitMQConfig.RK_COMPTE_REJETE,
                CompteOuvertEvent.builder()
                    .typeEvent("REJETE")
                    .compteId(saved.getId())
                    .clientId(saved.getClientId())
                    .clientEmail(saved.getClientEmail())
                    .clientNom(saved.getClientNom())
                    .numeroCompte(saved.getNumeroCompte())
                    .typeCompte(saved.getTypeCompte().name())
                    .timestamp(LocalDateTime.now())
                    .build()
            );
        }

        return mapToCompteResponse(saved);
    }

    //  INTERNES

    @Transactional
    public CompteResponse crediterInterne(Long compteId, BigDecimal montant) {
        Compte compte = findCompteOrThrow(compteId);
        compte.crediter(montant);
        log.info("interne de {} XAF sur le compte {}", montant, compte.getNumeroCompte());
        return mapToCompteResponse(compteRepository.save(compte));
    }

    @Transactional
    public CompteResponse debiterInterne(Long compteId, BigDecimal montant) {
        Compte compte = findCompteOrThrow(compteId);
        compte.debiter(montant);
        log.info("interne de {} XAF sur le compte {}", montant, compte.getNumeroCompte());
        return mapToCompteResponse(compteRepository.save(compte));
    }


    @Transactional(readOnly = true)
    public Page<CompteResponse> getComptesEnAttenteValidation(Pageable pageable) {
        return compteRepository.findByStatut(StatutCompte.EN_ATTENTE_VALIDATION, pageable)
                .map(this::mapToCompteResponse);
    }

    @Transactional(readOnly = true)
    public Page<CompteResponse> getComptesActifsByClientId(String clientId, Pageable pageable) {
        return compteRepository.findByClientIdAndStatut(clientId, StatutCompte.ACTIF, pageable)
                .map(this::mapToCompteResponse);
    }

    @Transactional(readOnly = true)
    public Page<CompteResponse> getComptesByType(TypeCompte typeCompte, Pageable pageable) {
        return compteRepository.findByTypeCompte(typeCompte, pageable).map(this::mapToCompteResponse);
    }

    @Transactional(readOnly = true)
    public long compterComptesActifs(String clientId) {
        return compteRepository.countByClientIdAndStatut(clientId, StatutCompte.ACTIF);
    }

    @Transactional(readOnly = true)
    public Page<CompteResponse> getComptesAvecSoldeSousMinimum(Pageable pageable) {
        return compteRepository.findComptesAvecSoldeSousMinimum(pageable).map(this::mapToCompteResponse);
    }

    @Transactional(readOnly = true)
    public Page<CompteResponse> rechercherComptes(
            String clientId, String numeroCompte, TypeCompte typeCompte,
            StatutCompte statut, BigDecimal soldeMin, BigDecimal soldeMax,
            Pageable pageable) {
        return compteRepository.findAll(
                org.example.accountservice.repository.CompteSpecification.withCriteria(
                        clientId, numeroCompte, typeCompte, statut, soldeMin, soldeMax),
                pageable
        ).map(this::mapToCompteResponse);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalSoldeClient(String clientId) {
        BigDecimal total = compteRepository.sumSoldeByClientId(clientId);
        return total != null ? total : BigDecimal.ZERO;
    }

    //  PRIVÉES

    private Compte findCompteOrThrow(Long id) {
        return compteRepository.findById(id).orElseThrow(() -> new CompteNotFoundException(id));
    }

    private String genererNumeroCompte() {
        int annee = LocalDateTime.now().getYear();
        long seq = refCounter.incrementAndGet();
        String numero = String.format("MFH-%d-%06d", annee, seq);

        while (compteRepository.existsByNumeroCompte(numero)) {
            seq = refCounter.incrementAndGet();
            numero = String.format("MFH-%d-%06d", annee, seq);
        }
        return numero;
    }

    private BigDecimal getTauxParDefaut(TypeCompte type) {
        return switch (type) {
            case EPARGNE       -> new BigDecimal("0.0350");
            case DEPOT_A_TERME -> new BigDecimal("0.0600");
            case MICRO_EPARGNE -> new BigDecimal("0.0200");
            default            -> BigDecimal.ZERO;
        };
    }

    private void publierEvenement(String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.COMPTE_EXCHANGE, routingKey, event);
        } catch (Exception ex) {
            log.error("publication RabbitMQ (routingKey: {}) : {}", routingKey, ex.getMessage());
        }
    }

    public CompteResponse mapToCompteResponse(Compte compte) {
        return CompteResponse.builder()
                .id(compte.getId())
                .clientId(compte.getClientId())
                .numeroCompte(compte.getNumeroCompte())
                .typeCompte(compte.getTypeCompte())
                .solde(compte.getSolde())
                .devise(compte.getDevise())
                .statut(compte.getStatut())
                .dateOuverture(compte.getDateOuverture())
                .tauxInteret(compte.getTauxInteret())
                .soldeMinimum(compte.getSoldeMinimum())
                .plafond(compte.getPlafond())
                .description(compte.getDescription())
                .createdAt(compte.getCreatedAt())
                .clientEmail(compte.getClientEmail())
                .clientNom(compte.getClientNom())
                .build();
    }
}
