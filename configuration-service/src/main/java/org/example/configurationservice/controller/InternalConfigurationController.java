package org.example.configurationservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.example.configurationservice.dto.reponse.AccountTypeConfigurationResponse;
import org.example.configurationservice.dto.reponse.MicrofinanceConfigurationResponse;
import org.example.configurationservice.model.AccountTypeConfiguration;
import org.example.configurationservice.repository.AccountTypeConfigurationRepository;
import org.example.configurationservice.service.AccountTypeConfigurationService;
import org.example.configurationservice.service.MicrofinanceConfigurationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/internal/configurations")
@RequiredArgsConstructor
public class InternalConfigurationController {
    
    private final MicrofinanceConfigurationService microfinanceConfigService;
    private final AccountTypeConfigurationService accountTypeConfigService;
    private final AccountTypeConfigurationRepository accountTypeConfigurationRepository;
    
    /**
     * Récupère la configuration microfinance active (pour Account Service)
     */
    @GetMapping("/microfinance/active")
    public ResponseEntity<MicrofinanceConfigurationResponse> getActiveMicrofinanceConfig() {
        log.info("Récupération de la configuration microfinance active");
        return ResponseEntity.ok(microfinanceConfigService.getActiveConfigurationResponse());
    }
    
    /**
     * Récupère la configuration du format de numéro de compte
     */
    @GetMapping("/account-number-format")
    public ResponseEntity<Map<String, Object>> getAccountNumberFormatConfig() {
        log.info("Récupération du format de numéro de compte");
        
        var formatRequest = microfinanceConfigService.getAccountNumberFormat();
        var config = microfinanceConfigService.getActiveConfiguration();
        
        Map<String, Object> response = new HashMap<>();
        response.put("useCustomFormat", formatRequest.isUseCustomFormat());
        response.put("accountNumberFormat", formatRequest.getAccountNumberFormat());
        response.put("bankCodeLength", formatRequest.getBankCodeLength());
        response.put("accountNumberLength", formatRequest.getAccountNumberLength());
        response.put("controlKeyLength", formatRequest.getControlKeyLength());
        response.put("separator", formatRequest.getSeparator());
        response.put("fixedPrefix", formatRequest.getFixedPrefix());
        response.put("fixedSuffix", formatRequest.getFixedSuffix());
        response.put("generationStrategy", formatRequest.getGenerationStrategy());
        response.put("includeCheckDigit", formatRequest.isIncludeCheckDigit());
        response.put("microfinanceCode", config.getMicrofinanceCode());
        response.put("agencyCode", config.getAgencyCode());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Récupère tous les types de comptes actifs
     */
    @GetMapping("/account-types/active")
    public ResponseEntity<List<AccountTypeConfigurationResponse>> getActiveAccountTypes() {
        log.info("Récupération des types de comptes actifs");
        return ResponseEntity.ok(accountTypeConfigService.getActiveAccountTypes());
    }
    
    /**
     * Récupère un type de compte par son code
     */
    @GetMapping("/account-types/by-code/{code}")
    public ResponseEntity<AccountTypeConfigurationResponse> getAccountTypeByCode(@PathVariable String code) {
        log.info("Récupération du type de compte par code: {}", code);
        return ResponseEntity.ok(accountTypeConfigService.getAccountTypeByCode(code));
    }
    
    /**
     * Récupère un type de compte par son type (SAVINGS, CURRENT, etc.)
     */
    @GetMapping("/account-types/by-type/{accountType}")
    public ResponseEntity<AccountTypeConfigurationResponse> getAccountTypeByType(@PathVariable String accountType) {
        log.info("Récupération du type de compte par type: {}", accountType);
        return ResponseEntity.ok(accountTypeConfigService.getAccountTypeByType(accountType));
    }
    
    /**
     * Test endpoint pour vérifier que le service est accessible
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        log.info("Test endpoint - Configuration service is running");
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "configuration-service");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * Génère un RIB (pour Account Service)
     */
    @PostMapping("/rib/generate")
    public ResponseEntity<Map<String, String>> generateRibInternal(
            @RequestParam String accountNumber,
            @RequestParam String clientId) {
        
        log.info("Génération de RIB - Compte: {}, Client: {}", accountNumber, clientId);
        
        String rib = microfinanceConfigService.generateRib(accountNumber, clientId);
        String formattedRib = microfinanceConfigService.formatRib(rib);
        
        Map<String, String> response = new HashMap<>();
        response.put("rib", rib);
        response.put("formattedRib", formattedRib);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Valide un RIB (pour Account Service)
     */
    @PostMapping("/rib/validate")
    public ResponseEntity<Map<String, Object>> validateRibInternal(@RequestParam String rib) {
        
        log.info("[INTERNAL] Validation de RIB: {}", rib);
        
        boolean isValid = microfinanceConfigService.validateRib(rib);
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);
        response.put("message", isValid ? "RIB valide" : "RIB invalide");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Génère un numéro de compte complet (pour Account Service)
     */
    @PostMapping("/rib/generate-full-account")
    public ResponseEntity<Map<String, String>> generateFullAccountNumberInternal(@RequestParam String clientId) {
        
        log.info("Génération de compte complet pour client: {}", clientId);
        
        String fullAccountNumber = microfinanceConfigService.generateFullAccountNumber(clientId);
        String formattedRib = microfinanceConfigService.formatRib(fullAccountNumber);
        
        Map<String, String> response = new HashMap<>();
        response.put("fullAccountNumber", fullAccountNumber);
        response.put("formattedAccountNumber", formattedRib);
        
        return ResponseEntity.ok(response);
    }

    /**
     * CORRIGÉ : Récupère un type de compte par son nom (insensible à la casse)
     */
    @GetMapping("/account-types/by-name/{name}")
    public ResponseEntity<AccountTypeConfigurationResponse> getAccountTypeByName(@PathVariable String name) {
        log.info(" [INTERNAL] Recherche du type de compte par nom: {}", name);
        
        AccountTypeConfiguration config = accountTypeConfigurationRepository
            .findByNameIgnoreCase(name)
            .orElseThrow(() -> new RuntimeException("Type de compte non trouvé: " + name));
        
        return ResponseEntity.ok(mapToResponse(config));
    }
    
    /**
     *  MÉTHODE mapToResponse - Convertit AccountTypeConfiguration en AccountTypeConfigurationResponse
     */
    private AccountTypeConfigurationResponse mapToResponse(AccountTypeConfiguration config) {
        if (config == null) {
            return null;
        }
        
        return AccountTypeConfigurationResponse.builder()
                .id(config.getId())
                .code(config.getCode())
                .accountType(config.getAccountType())
                .category(mapToCategoryResponse(config.getCategory()))
                .name(config.getName())
                .description(config.getDescription())
                .minimumBalance(config.getMinimumBalance())
                .maximumBalance(config.getMaximumBalance())
                .interestRate(config.getInterestRate())
                .monthlyFee(config.getMonthlyFee())
                .transactionFee(config.getTransactionFee())
                .allowOverdraft(config.isAllowOverdraft())
                .overdraftLimit(config.getOverdraftLimit())
                .active(config.isActive())
                .maxAccountsPerClient(config.getMaxAccountsPerClient())
                .isDefault(config.isDefault())
                .eligibilityCriteria(config.getEligibilityCriteria())
                .benefits(config.getBenefits())
                .requiredDocuments(config.getRequiredDocuments())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
    
    /**
     * Convertit la catégorie en réponse
     */
    private org.example.configurationservice.dto.reponse.AccountCategoryResponse mapToCategoryResponse(
            org.example.configurationservice.model.AccountCategory category) {
        if (category == null) {
            return null;
        }
        
        return org.example.configurationservice.dto.reponse.AccountCategoryResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .description(category.getDescription())
                .icon(category.getIcon())
                .color(category.getColor())
                .displayOrder(category.getDisplayOrder())
                .active(category.isActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    @GetMapping("/test/account-number/{agencyCode}")
    public ResponseEntity<Map<String, String>> testAccountNumberFormat(
         @PathVariable String agencyCode,
         @RequestParam String clientId) {
    
         log.info("de formatage pour agence: {}, client: {}", agencyCode, clientId);
    
         MicrofinanceConfigurationResponse config = microfinanceConfigService.getActiveConfigurationResponse();
    
         String bankCode = config.getMicrofinanceCode();
         String paddedAgencyCode = String.format("%05d", Integer.parseInt(agencyCode));
         String accountNumber = String.format("%011d", System.currentTimeMillis() % 100000000000L);
         String controlKey = "78";
    
         String fullNumber = bankCode + paddedAgencyCode + accountNumber + controlKey;
         String formatted = bankCode + "-" + paddedAgencyCode + "-" + accountNumber + "-" + controlKey;
    
         Map<String, String> response = new HashMap<>();
         response.put("raw", fullNumber);
         response.put("formatted", formatted);
         response.put("bankCode", bankCode);
         response.put("agencyCode", paddedAgencyCode);
         response.put("accountNumber", accountNumber);
         response.put("controlKey", controlKey);
    
         return ResponseEntity.ok(response);
    }
}