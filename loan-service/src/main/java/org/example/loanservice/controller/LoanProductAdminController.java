package org.example.loanservice.controller;

import org.example.loanservice.dto.equest.LoanProductRequest;
import org.example.loanservice.dto.response.LoanProductResponse;
import org.example.loanservice.service.LoanProductService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/loan-products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")  // l'admin peut accéder à ce controller
public class LoanProductAdminController {
    
    private final LoanProductService loanProductService;
    
    @PostMapping
    @Operation(summary = "Créer un nouveau produit de prêt (Admin uniquement)")
    public ResponseEntity<LoanProductResponse> createProduct(
            @Valid @RequestBody LoanProductRequest request,
            @RequestHeader("X-User-Id") String userId) {
        LoanProductResponse response = loanProductService.createProduct(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Modifier un produit de prêt (Admin uniquement)")
    public ResponseEntity<LoanProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody LoanProductRequest request,
            @RequestHeader("X-User-Id") String userId) {
        LoanProductResponse response = loanProductService.updateProduct(id, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}/status")
    @Operation(summary = "Activer/Désactiver un produit (Admin uniquement)")
    public ResponseEntity<LoanProductResponse> toggleProductStatus(
            @PathVariable String id,
            @RequestParam boolean active,
            @RequestHeader("X-User-Id") String userId) {
        LoanProductResponse response = loanProductService.toggleProductStatus(id, active, userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un produit par son ID")
    public ResponseEntity<LoanProductResponse> getProduct(@PathVariable String id) {
        return ResponseEntity.ok(loanProductService.getProduct(id));
    }
    
    // ("/code/{code}")
    // (summary = "Récupérer un produit par son code")
    //  ResponseEntity<LoanProductResponse> getProductByCode(@PathVariable String code) {
    //      ResponseEntity.ok(loanProductService.getProductByCode(code));
    // 
    
    @GetMapping("/active")
    @Operation(summary = "Lister tous les produits actifs")
    public ResponseEntity<List<LoanProductResponse>> getAllActiveProducts() {
        return ResponseEntity.ok(loanProductService.getAllActiveProducts());
    }
    
    @GetMapping
    @Operation(summary = "Lister tous les produits (paginé)")
    public ResponseEntity<Page<LoanProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, size, sort);
        
        return ResponseEntity.ok(loanProductService.getAllProducts(pageable));
    }
    
    @GetMapping("/active/paginated")
    @Operation(summary = "Lister les produits actifs (paginé)")
    public ResponseEntity<Page<LoanProductResponse>> getActiveProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(loanProductService.getActiveProducts(pageable));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un produit (Admin uniquement)")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        loanProductService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}