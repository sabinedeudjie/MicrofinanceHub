package org.example.configurationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.example.configurationservice.dto.reponse.AccountCategoryResponse;
import org.example.configurationservice.dto.reponse.AccountTypeConfigurationResponse;
import org.example.configurationservice.dto.reponse.MicrofinanceConfigurationResponse;
import org.example.configurationservice.dto.reponse.TestAccountNumberResponse;
import org.example.configurationservice.dto.request.AccountCategoryRequest;
import org.example.configurationservice.dto.request.AccountNumberFormatRequest;
import org.example.configurationservice.dto.request.AccountTypeConfigurationRequest;
import org.example.configurationservice.dto.request.MicrofinanceConfigurationRequest;
import org.example.configurationservice.service.AccountCategoryService;
import org.example.configurationservice.service.AccountTypeConfigurationService;
import org.example.configurationservice.service.MicrofinanceConfigurationService;
import org.example.configurationservice.service.RibGeneratorService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/configurations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminConfigurationController {
    
    private final MicrofinanceConfigurationService microfinanceConfigService;
    private final AccountTypeConfigurationService accountTypeConfigService;
    private final AccountCategoryService accountCategoryService;
    private final RibGeneratorService ribGeneratorService;
    
    // 
    //  DU FORMAT DE NUMÉRO DE COMPTE
    // 
    
    @PostMapping("/account-number-format")
    public ResponseEntity<MicrofinanceConfigurationResponse> configureAccountNumberFormat(
            @Valid @RequestBody AccountNumberFormatRequest request) {
        log.info("du format de numéro de compte");
        MicrofinanceConfigurationResponse response = microfinanceConfigService.configureAccountNumberFormat(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/account-number-format")
    public ResponseEntity<AccountNumberFormatRequest> getAccountNumberFormat() {
        log.info("du format de numéro de compte");
        AccountNumberFormatRequest response = microfinanceConfigService.getAccountNumberFormat();
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/test-account-number")
    public ResponseEntity<TestAccountNumberResponse> testAccountNumberGeneration(@RequestParam String clientId) {
        log.info("de génération de numéro de compte pour client: {}", clientId);
        String accountNumber = microfinanceConfigService.testAccountNumberGeneration(clientId);
        String formatted = microfinanceConfigService.formatAccountNumber(accountNumber);
        
        TestAccountNumberResponse response = TestAccountNumberResponse.builder()
                .raw(accountNumber)
                .formatted(formatted)
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/test-account-number-map")
    public ResponseEntity<Map<String, String>> testAccountNumberGenerationMap(@RequestParam String clientId) {
        log.info("de génération de numéro de compte (Map) pour client: {}", clientId);
        String accountNumber = microfinanceConfigService.testAccountNumberGeneration(clientId);
        String formatted = microfinanceConfigService.formatAccountNumber(accountNumber);
        
        Map<String, String> response = new HashMap<>();
        response.put("raw", accountNumber);
        response.put("formatted", formatted);
        
        return ResponseEntity.ok(response);
    }
    
    // 
    //  MICROFINANCE
    // 
    
    @PostMapping("/microfinance")
    public ResponseEntity<MicrofinanceConfigurationResponse> createOrUpdateConfig(
            @Valid @RequestBody MicrofinanceConfigurationRequest request) {
        String createdBy = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("/mise à jour de la configuration microfinance par: {}", createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(microfinanceConfigService.saveConfiguration(request, createdBy));
    }
    
    @GetMapping("/microfinance")
    public ResponseEntity<MicrofinanceConfigurationResponse> getActiveConfig() {
        log.info("de la configuration microfinance active");
        return ResponseEntity.ok(microfinanceConfigService.getActiveConfigurationResponse());
    }
    
    @PostMapping("/client-id/generate")
    public ResponseEntity<String> generateClientId(@RequestParam(required = false) String clientEmail) {
        log.info("d'ID client pour email: {}", clientEmail);
        return ResponseEntity.ok(microfinanceConfigService.generateClientId(clientEmail));
    }
    
    @PostMapping("/client-id/validate")
    public ResponseEntity<Boolean> validateCustomClientId(@RequestParam String customId) {
        log.info("d'ID client personnalisé: {}", customId);
        return ResponseEntity.ok(microfinanceConfigService.validateCustomClientId(customId));
    }

    /**
     *  MODIFIER une configuration microfinance existante
     * PUT /api/admin/configurations/microfinance/{id}
     */
    @PutMapping("/microfinance/{id}")
    public ResponseEntity<MicrofinanceConfigurationResponse> updateConfiguration(
            @PathVariable String id,
            @Valid @RequestBody MicrofinanceConfigurationRequest request) {
        String updatedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("à jour de la configuration microfinance ID: {} par: {}", id, updatedBy);
        return ResponseEntity.ok(microfinanceConfigService.updateConfiguration(id, request, updatedBy));
    }
    
    /**
     *  Récupérer une configuration par ID
     * GET /api/admin/configurations/microfinance/{id}
     */
    @GetMapping("/microfinance/{id}")
    public ResponseEntity<MicrofinanceConfigurationResponse> getConfigurationById(@PathVariable String id) {
        log.info("de la configuration microfinance ID: {}", id);
        return ResponseEntity.ok(microfinanceConfigService.getConfigurationById(id));
    }
    
    /**
     *  Activer/Désactiver une configuration
     * PATCH /api/admin/configurations/microfinance/{id}/toggle
     */
    @PatchMapping("/microfinance/{id}/toggle")
    public ResponseEntity<MicrofinanceConfigurationResponse> toggleConfigurationActive(@PathVariable String id) {
        log.info("/désactivation de la configuration microfinance ID: {}", id);
        return ResponseEntity.ok(microfinanceConfigService.toggleConfigurationActive(id));
    }
    
    /**
     *  Récupérer toutes les configurations
     * GET /api/admin/configurations/microfinance/all
     */
    @GetMapping("/microfinance/all")
    public ResponseEntity<List<MicrofinanceConfigurationResponse>> getAllConfigurations() {
        log.info("de toutes les configurations microfinance");
        return ResponseEntity.ok(microfinanceConfigService.getAllConfigurations());
    }
    
    // 
    //  DES TYPES DE COMPTES
    // 
    
    @PostMapping("/account-types")
    public ResponseEntity<AccountTypeConfigurationResponse> createAccountType(
            @Valid @RequestBody AccountTypeConfigurationRequest request) {
        String createdBy = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("d'un type de compte par: {}", createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountTypeConfigService.createAccountType(request, createdBy));
    }
    
    @PutMapping("/account-types/{id}")
    public ResponseEntity<AccountTypeConfigurationResponse> updateAccountType(
            @PathVariable String id,
            @Valid @RequestBody AccountTypeConfigurationRequest request) {
        String updatedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("à jour du type de compte {} par: {}", id, updatedBy);
        return ResponseEntity.ok(accountTypeConfigService.updateAccountType(id, request, updatedBy));
    }
    
    @DeleteMapping("/account-types/{id}")
    public ResponseEntity<Void> deleteAccountType(@PathVariable String id) {
        log.info("du type de compte: {}", id);
        accountTypeConfigService.deleteAccountType(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/account-types")
    public ResponseEntity<List<AccountTypeConfigurationResponse>> getAllAccountTypes() {
        log.info("de tous les types de comptes");
        return ResponseEntity.ok(accountTypeConfigService.getAllAccountTypes());
    }
    
    @GetMapping("/account-types/{id}")
    public ResponseEntity<AccountTypeConfigurationResponse> getAccountType(@PathVariable String id) {
        log.info("du type de compte: {}", id);
        return ResponseEntity.ok(accountTypeConfigService.getAccountType(id));
    }
    
    @PatchMapping("/account-types/{id}/toggle")
    public ResponseEntity<AccountTypeConfigurationResponse> toggleAccountTypeActive(@PathVariable String id) {
        log.info("/désactivation du type de compte: {}", id);
        return ResponseEntity.ok(accountTypeConfigService.toggleActive(id));
    }
    
    // 
    //  DES CATÉGORIES DE COMPTES
    // 
    
    @PostMapping("/categories")
    public ResponseEntity<AccountCategoryResponse> createCategory(
            @Valid @RequestBody AccountCategoryRequest request) {
        String createdBy = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("d'une catégorie par: {}", createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountCategoryService.createCategory(request, createdBy));
    }
    
    @PutMapping("/categories/{id}")
    public ResponseEntity<AccountCategoryResponse> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody AccountCategoryRequest request) {
        String updatedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("à jour de la catégorie {} par: {}", id, updatedBy);
        return ResponseEntity.ok(accountCategoryService.updateCategory(id, request, updatedBy));
    }
    
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable String id) {
        log.info("de la catégorie: {}", id);
        accountCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/categories")
    public ResponseEntity<List<AccountCategoryResponse>> getAllCategories() {
        log.info("de toutes les catégories");
        return ResponseEntity.ok(accountCategoryService.getAllCategories());
    }
    
    @GetMapping("/categories/active")
    public ResponseEntity<List<AccountCategoryResponse>> getActiveCategories() {
        log.info("des catégories actives");
        return ResponseEntity.ok(accountCategoryService.getActiveCategories());
    }
    
    @GetMapping("/categories/{id}")
    public ResponseEntity<AccountCategoryResponse> getCategory(@PathVariable String id) {
        log.info("de la catégorie: {}", id);
        return ResponseEntity.ok(accountCategoryService.getCategory(id));
    }
    
    @PatchMapping("/categories/{id}/toggle")
    public ResponseEntity<AccountCategoryResponse> toggleCategoryActive(@PathVariable String id) {
        log.info("/désactivation de la catégorie: {}", id);
        return ResponseEntity.ok(accountCategoryService.toggleActive(id));
    }

    // 
    //  POUR RIB
    // 
    
    /**
     * Génère un RIB pour un compte
     */
    @PostMapping("/rib/generate")
    public ResponseEntity<Map<String, String>> generateRib(
            @RequestParam String accountNumber,
            @RequestParam String clientId) {
        
        log.info("de RIB - Compte: {}, Client: {}", accountNumber, clientId);
        
        String rib = microfinanceConfigService.generateRib(accountNumber, clientId);
        String formattedRib = microfinanceConfigService.formatRib(rib);
        
        Map<String, String> response = new HashMap<>();
        response.put("rib", rib);
        response.put("formattedRib", formattedRib);
        response.put("accountNumber", accountNumber);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Valide un RIB
     */
    @PostMapping("/rib/validate")
    public ResponseEntity<Map<String, Object>> validateRib(@RequestParam String rib) {
        
        log.info("de RIB: {}", rib);
        
        boolean isValid = microfinanceConfigService.validateRib(rib);
        String formattedRib = microfinanceConfigService.formatRib(rib);
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);
        response.put("rib", rib);
        response.put("formattedRib", formattedRib);
        response.put("message", isValid ? "RIB valide" : "RIB invalide");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Génère un numéro de compte complet avec RIB
     */
    @PostMapping("/rib/generate-full-account")
    public ResponseEntity<Map<String, String>> generateFullAccountNumber(@RequestParam String clientId) {
        
        log.info("de compte complet pour client: {}", clientId);
        
        String fullAccountNumber = microfinanceConfigService.generateFullAccountNumber(clientId);
        String formattedRib = microfinanceConfigService.formatRib(fullAccountNumber);
        
        Map<String, String> response = new HashMap<>();
        response.put("fullAccountNumber", fullAccountNumber);
        response.put("formattedAccountNumber", formattedRib);
        response.put("clientId", clientId);
        
        return ResponseEntity.ok(response);
    }

    
}