package org.example.accountservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.accountservice.dto.*;
import org.example.accountservice.model.StatutCompte;
import org.example.accountservice.model.TypeCompte;
import org.example.accountservice.service.CompteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/comptes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Comptes", description = "Gestion des comptes bancaires")
@SecurityRequirement(name = "BearerAuth")
public class CompteController {

    private final CompteService compteService;

    // 
    //  DES COMPTES
    // 

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<ApiResponse<CompteResponse>> ouvrirCompte(
            @Valid @RequestBody OuvrirCompteRequest request) {
        CompteResponse compte = compteService.ouvrirCompte(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Compte ouvert avec succès. En attente de validation.", compte));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompteResponse>> getCompte(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Compte trouvé", compteService.getCompteById(id)));
    }

    @GetMapping("/numero/{numeroCompte}")
    public ResponseEntity<ApiResponse<CompteResponse>> getCompteByNumero(@PathVariable String numeroCompte) {
        return ResponseEntity.ok(ApiResponse.success("Compte trouvé", compteService.getCompteByNumero(numeroCompte)));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<ApiResponse<Page<CompteResponse>>> getComptesByClient(
            @PathVariable String clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CompteResponse> comptes = compteService.getComptesByClientId(clientId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(comptes.getTotalElements() + " compte(s) trouvé(s)", comptes));
    }

    @GetMapping("/client/{clientId}/actifs")
    public ResponseEntity<ApiResponse<Page<CompteResponse>>> getComptesActifsByClient(
            @PathVariable String clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CompteResponse> comptes = compteService.getComptesActifsByClientId(clientId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(comptes.getTotalElements() + " compte(s) actif(s)", comptes));
    }

    @GetMapping("/client/{clientId}/comptes-actifs-count")
    public ResponseEntity<ApiResponse<Long>> compterComptesActifs(@PathVariable String clientId) {
        return ResponseEntity.ok(ApiResponse.success("Nombre de comptes actifs", compteService.compterComptesActifs(clientId)));
    }

    @GetMapping("/client/{clientId}/solde-total")
    public ResponseEntity<ApiResponse<BigDecimal>> getSoldeTotalClient(@PathVariable String clientId) {
        return ResponseEntity.ok(ApiResponse.success("Solde total consolidé (XAF)", compteService.getTotalSoldeClient(clientId)));
    }

    @GetMapping("/en-attente-validation")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<ApiResponse<Page<CompteResponse>>> getComptesEnAttenteValidation(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CompteResponse> comptes = compteService.getComptesEnAttenteValidation(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(comptes.getTotalElements() + " dossier(s) en attente", comptes));
    }

    @GetMapping("/{id}/solde")
    public ResponseEntity<ApiResponse<BigDecimal>> consulterSolde(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Solde actuel", compteService.consulterSolde(id)));
    }

    @GetMapping("/type/{typeCompte}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<CompteResponse>>> getComptesByType(
            @PathVariable TypeCompte typeCompte,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CompteResponse> comptes = compteService.getComptesByType(typeCompte, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(comptes.getTotalElements() + " compte(s) de type " + typeCompte, comptes));
    }

    @GetMapping("/alertes/solde-minimum")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<CompteResponse>>> getComptesAvecSoldeSousMinimum(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CompteResponse> comptes = compteService.getComptesAvecSoldeSousMinimum(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(comptes.getTotalElements() + " compte(s) sous le solde minimum", comptes));
    }

    @GetMapping("/recherche")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<CompteResponse>>> rechercherComptes(
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String numeroCompte,
            @RequestParam(required = false) TypeCompte typeCompte,
            @RequestParam(required = false) StatutCompte statut,
            @RequestParam(required = false) BigDecimal soldeMin,
            @RequestParam(required = false) BigDecimal soldeMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CompteResponse> comptes = compteService.rechercherComptes(
                clientId, numeroCompte, typeCompte, statut, soldeMin, soldeMax, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(comptes.getTotalElements() + " compte(s) trouvé(s)", comptes));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<CompteResponse>> modifierCompte(
            @PathVariable Long id,
            @Valid @RequestBody ModifierCompteRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Compte mis à jour", compteService.modifierCompte(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> supprimerCompte(@PathVariable Long id) {
        compteService.supprimerCompte(id);
        return ResponseEntity.ok(ApiResponse.success("Compte supprimé définitivement", null));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<ApiResponse<CompteResponse>> changerStatut(
            @PathVariable Long id,
            @RequestParam StatutCompte statut) {
        return ResponseEntity.ok(ApiResponse.success("Statut mis à jour : " + statut, compteService.changerStatut(id, statut)));
    }

    // 
    //  INTERNES — réservés à transaction-service
    // 

    @Operation(
        summary = "Créditer un compte [INTERNE]",
        description = "Endpoint interne appelé par transaction-service après confirmation d'un dépôt. Accès : ADMIN uniquement."
    )
    @PostMapping("/{id}/crediter")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CompteResponse>> crediterInterne(
            @PathVariable Long id,
            @Parameter(description = "Montant à créditer en XAF") @RequestParam BigDecimal montant) {
        log.info("/api/comptes/{}/crediter - Montant: {} XAF [service interne]", id, montant);
        return ResponseEntity.ok(ApiResponse.success("Compte crédité", compteService.crediterInterne(id, montant)));
    }

    @Operation(
        summary = "Débiter un compte [INTERNE]",
        description = "Endpoint interne appelé par transaction-service lors d'un retrait ou virement. Accès : ADMIN uniquement."
    )
    @PostMapping("/{id}/debiter")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CompteResponse>> debiterInterne(
            @PathVariable Long id,
            @Parameter(description = "Montant à débiter en XAF") @RequestParam BigDecimal montant) {
        log.info("/api/comptes/{}/debiter - Montant: {} XAF [service interne]", id, montant);
        return ResponseEntity.ok(ApiResponse.success("Compte débité", compteService.debiterInterne(id, montant)));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Service Compte opérationnel", "OK"));
    }
}
