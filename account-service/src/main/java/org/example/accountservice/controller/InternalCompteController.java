package org.example.accountservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.accountservice.model.Compte;
import org.example.accountservice.model.StatutCompte;
import org.example.accountservice.repository.CompteRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class InternalCompteController {

    private final CompteRepository compteRepository;

    /** Stats globales des comptes — appelé par agency-service via Feign */
    @GetMapping("/api/accounts/internal/stats/by-agency/{agencyId}")
    public ResponseEntity<Map<String, Object>> getStatsByAgency(@PathVariable String agencyId) {
        long totalAccounts  = compteRepository.count();
        long totalClients   = compteRepository.countDistinctClients();
        BigDecimal totalBalance = compteRepository.sumAllSoldesActifs();

        Map<String, Object> stats = new HashMap<>();
        stats.put("agencyId",         agencyId);
        stats.put("totalAccounts",    totalAccounts);
        stats.put("totalClients",     totalClients);
        stats.put("totalOutstanding", totalBalance);
        return ResponseEntity.ok(stats);
    }

    /** Vérifie si le client a au moins un compte actif */
    @GetMapping("/api/internal/accounts/client/{clientId}/exists")
    public ResponseEntity<Boolean> accountExists(@PathVariable String clientId) {
        boolean exists = compteRepository.countByClientIdAndStatut(clientId, StatutCompte.ACTIF) > 0;
        return ResponseEntity.ok(exists);
    }

    /** Retourne le statut du premier compte actif du client */
    @GetMapping("/api/internal/accounts/client/{clientId}/status")
    public ResponseEntity<String> getAccountStatus(@PathVariable String clientId) {
        return compteRepository.findByClientIdAndStatut(clientId, StatutCompte.ACTIF, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(c -> ResponseEntity.ok(c.getStatut().name()))
                .orElse(ResponseEntity.ok("NONE"));
    }

    /** Retourne les infos d'un compte par son numéro */
    @GetMapping("/api/internal/accounts/number/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getAccountByNumber(@PathVariable String accountNumber) {
        return compteRepository.findByNumeroCompte(accountNumber)
                .map(c -> ResponseEntity.ok(toAccountInfo(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Retourne tous les comptes d'un client */
    @GetMapping("/api/internal/accounts/client/{clientId}/all")
    public ResponseEntity<List<Map<String, Object>>> getAccountsByClientId(@PathVariable String clientId) {
        List<Map<String, Object>> accounts = compteRepository
                .findByClientId(clientId, PageRequest.of(0, 50))
                .stream()
                .map(this::toAccountInfo)
                .collect(Collectors.toList());
        return ResponseEntity.ok(accounts);
    }

    /** Vérifie qu'un compte appartient bien à un client donné */
    @GetMapping("/api/internal/accounts/validate/{accountNumber}/client/{clientId}")
    public ResponseEntity<Boolean> validateAccountOwnership(
            @PathVariable String accountNumber,
            @PathVariable String clientId) {
        return compteRepository.findByNumeroCompte(accountNumber)
                .map(c -> ResponseEntity.ok(c.getClientId().equals(clientId)))
                .orElse(ResponseEntity.ok(false));
    }

    private String mapStatut(StatutCompte s) {
        return switch (s) {
            case ACTIF                  -> "ACTIVE";
            case INACTIF                -> "INACTIVE";
            case SUSPENDU               -> "SUSPENDED";
            case BLOQUE                 -> "BLOCKED";
            case FERME                  -> "CLOSED";
            case EN_ATTENTE_VALIDATION  -> "PENDING";
            case REJETE                 -> "REJECTED";
        };
    }

    private Map<String, Object> toAccountInfo(Compte c) {
        Map<String, Object> info = new HashMap<>();
        info.put("id",            c.getId().toString());
        info.put("accountNumber", c.getNumeroCompte());
        info.put("clientId",      c.getClientId());
        info.put("accountName",   c.getTypeCompte().name());
        info.put("accountType",   c.getTypeCompte().name());
        info.put("agencyId",      null);
        info.put("agencyCode",    null);
        info.put("agencyName",    null);
        info.put("balance",       c.getSolde() != null ? c.getSolde() : BigDecimal.ZERO);
        info.put("currency",      c.getDevise() != null ? c.getDevise().name() : "XAF");
        info.put("status",        mapStatut(c.getStatut()));
        info.put("description",   c.getDescription());
        info.put("clientEmail",   c.getClientEmail());
        info.put("clientNom",     c.getClientNom());
        return info;
    }
}
