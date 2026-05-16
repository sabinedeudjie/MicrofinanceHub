package org.example.transactionservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionservice.dto.*;
import org.example.transactionservice.model.StatutTransaction;
import org.example.transactionservice.model.TypeTransaction;
import org.example.transactionservice.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transactions", description = "Opérations financières : dépôts, retraits, virements")
@SecurityRequirement(name = "BearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "Effectuer un dépôt")
    @PostMapping("/depot/{compteId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> effectuerDepot(
            @PathVariable Long compteId,
            @Valid @RequestBody DepotRequest request) {

        log.info("/api/transactions/depot/{} - Montant: {} XAF", compteId, request.getMontant());
        TransactionResponse tx = transactionService.effectuerDepot(compteId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Dépôt de " + request.getMontant() + " XAF effectué", tx));
    }

    @Operation(summary = "Effectuer un retrait")
    @PostMapping("/retrait/{compteId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> effectuerRetrait(
            @PathVariable Long compteId,
            @Valid @RequestBody RetraitRequest request) {

        log.info("/api/transactions/retrait/{} - Montant: {} XAF", compteId, request.getMontant());
        TransactionResponse tx = transactionService.effectuerRetrait(compteId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Retrait de " + request.getMontant() + " XAF effectué", tx));
    }

    @Operation(summary = "Effectuer un virement")
    @PostMapping("/virement/{compteId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> effectuerVirement(
            @PathVariable Long compteId,
            @Valid @RequestBody VirementRequest request) {

        log.info("/api/transactions/virement/{} → {}", compteId, request.getNumeroCompteDestination());
        TransactionResponse tx = transactionService.effectuerVirement(compteId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Virement de " + request.getMontant() + " XAF effectué", tx));
    }

    @Operation(summary = "Historique des transactions d'un compte")
    @GetMapping("/compte/{compteId}")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getHistorique(
            @PathVariable Long compteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<TransactionResponse> transactions = transactionService.getHistorique(compteId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(transactions.getTotalElements() + " transaction(s)", transactions));
    }

    @Operation(summary = "Transactions d'un compte sur une période")
    @GetMapping("/compte/{compteId}/periode")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getByPeriode(
            @PathVariable Long compteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<TransactionResponse> transactions = transactionService.getByPeriode(compteId, debut, fin, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(transactions.getTotalElements() + " transaction(s) sur la période", transactions));
    }

    @Operation(summary = "Transactions d'un compte par type")
    @GetMapping("/compte/{compteId}/type/{type}")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getByType(
            @PathVariable Long compteId,
            @PathVariable TypeTransaction type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<TransactionResponse> transactions = transactionService.getByType(compteId, type, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(transactions.getTotalElements() + " transaction(s) de type " + type, transactions));
    }

    @Operation(summary = "Compter les transactions d'un compte")
    @GetMapping("/compte/{compteId}/count")
    public ResponseEntity<ApiResponse<Long>> compterTransactions(@PathVariable Long compteId) {
        return ResponseEntity.ok(ApiResponse.success("Nombre de transactions", transactionService.compterTransactions(compteId)));
    }

    @Operation(summary = "Transactions par statut — supervision")
    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getByStatut(
            @PathVariable StatutTransaction statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<TransactionResponse> transactions = transactionService.getByStatut(statut, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(transactions.getTotalElements() + " transaction(s) avec statut " + statut, transactions));
    }

    @Operation(summary = "Rechercher des transactions — multicritères")
    @GetMapping("/recherche")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> rechercher(
            @RequestParam(required = false) Long compteId,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) TypeTransaction typeTransaction,
            @RequestParam(required = false) StatutTransaction statut,
            @RequestParam(required = false) BigDecimal montantMin,
            @RequestParam(required = false) BigDecimal montantMax,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("/api/transactions/recherche - compteId={}, type={}, statut={}", compteId, typeTransaction, statut);
        Page<TransactionResponse> transactions = transactionService.rechercher(
                compteId, reference, typeTransaction, statut, montantMin, montantMax, debut, fin,
                PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(transactions.getTotalElements() + " transaction(s) trouvée(s)", transactions));
    }

    @Operation(summary = "Relancer les transactions bloquées via CamPay")
    @PostMapping("/relance")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> relancer() {
        int traitees = transactionService.relancerTransactionsEnAttente();
        return ResponseEntity.ok(ApiResponse.success(traitees + " transaction(s) mises à jour", traitees));
    }

    @Operation(summary = "Enregistrer un remboursement de prêt (tous modes)")
    @PostMapping("/remboursement-pret")
    public ResponseEntity<ApiResponse<TransactionResponse>> remboursementPret(
            @Valid @RequestBody org.example.transactionservice.dto.RemboursementPretRequest request) {

        log.info("/api/transactions/remboursement-pret - loanId={}, mode={}, montant={}",
                request.getLoanId(), request.getModePaiement(), request.getMontant());
        TransactionResponse tx = transactionService.effectuerRemboursementPret(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Remboursement enregistré", tx));
    }

    @Operation(summary = "Santé du service")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Service Transaction opérationnel", "OK"));
    }
}
