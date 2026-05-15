package org.example.configurationservice.service;

import org.example.configurationservice.dto.reponse.AccountCategoryResponse;
import org.example.configurationservice.dto.reponse.AccountTypeConfigurationResponse;
import org.example.configurationservice.dto.request.AccountTypeConfigurationRequest;
import org.example.configurationservice.exceptions.AccountTypeNotFoundException;
import org.example.configurationservice.exceptions.CategoryNotFoundException;
import org.example.configurationservice.exceptions.DuplicateCodeException;
import org.example.configurationservice.model.AccountCategory;
import org.example.configurationservice.model.AccountTypeConfiguration;
import org.example.configurationservice.model.enums.AccountType;
import org.example.configurationservice.repository.AccountCategoryRepository;
import org.example.configurationservice.repository.AccountTypeConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountTypeConfigurationService {
    
    private final AccountTypeConfigurationRepository typeConfigRepository;
    private final AccountCategoryRepository categoryRepository;
    
    @Transactional
    public AccountTypeConfigurationResponse createAccountType(AccountTypeConfigurationRequest request, String createdBy) {
        log.info("du type de compte: {}", request.getCode());
        
        if (typeConfigRepository.existsByCode(request.getCode())) {
            throw new DuplicateCodeException(
                String.format("Le code '%s' est déjà utilisé par un autre type de compte.", request.getCode())
            );
        }
        
        AccountCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(
                    String.format("Catégorie non trouvée avec l'ID: '%s'", request.getCategoryId())
                ));
        
        if (request.isDefault()) {
            typeConfigRepository.findByCategoryIdAndIsDefaultTrue(category.getId())
                    .ifPresent(defaultConfig -> {
                        defaultConfig.setDefault(false);
                        typeConfigRepository.save(defaultConfig);
                    });
        }
        
        AccountTypeConfiguration config = AccountTypeConfiguration.builder()
                .code(request.getCode())
                .accountType(request.getAccountType())
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .minimumBalance(request.getMinimumBalance())
                .maximumBalance(request.getMaximumBalance())
                .interestRate(request.getInterestRate())
                .monthlyFee(request.getMonthlyFee())
                .transactionFee(request.getTransactionFee())
                .allowOverdraft(request.isAllowOverdraft())
                .overdraftLimit(request.getOverdraftLimit())
                .maxAccountsPerClient(request.getMaxAccountsPerClient())
                .isDefault(request.isDefault())
                .eligibilityCriteria(request.getEligibilityCriteria())
                .benefits(request.getBenefits())
                .requiredDocuments(request.getRequiredDocuments())
                .active(true)
                .createdBy(createdBy)
                .build();
        
        try {
            config = typeConfigRepository.save(config);
            log.info("de compte créé: {} - {}", config.getCode(), config.getName());
            return mapToResponse(config);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("Erreur lors de la création: Violation de contrainte. Vérifiez que toutes les données sont valides.", e);
        }
    }
    
    @Transactional
    public AccountTypeConfigurationResponse updateAccountType(String id, AccountTypeConfigurationRequest request, String updatedBy) {
        log.info("à jour du type de compte: {}", id);
        
        AccountTypeConfiguration config = typeConfigRepository.findById(id)
                .orElseThrow(() -> new AccountTypeNotFoundException("Type de compte non trouvé: " + id));
        
        AccountCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Catégorie non trouvée: " + request.getCategoryId()));
        
        if (request.isDefault() && !config.isDefault()) {
            typeConfigRepository.findByCategoryIdAndIsDefaultTrue(category.getId())
                    .ifPresent(defaultConfig -> {
                        defaultConfig.setDefault(false);
                        typeConfigRepository.save(defaultConfig);
                    });
        }
        
        config.setCode(request.getCode());
        config.setAccountType(request.getAccountType());
        config.setCategory(category);
        config.setName(request.getName());
        config.setDescription(request.getDescription());
        config.setMinimumBalance(request.getMinimumBalance());
        config.setMaximumBalance(request.getMaximumBalance());
        config.setInterestRate(request.getInterestRate());
        config.setMonthlyFee(request.getMonthlyFee());
        config.setTransactionFee(request.getTransactionFee());
        config.setAllowOverdraft(request.isAllowOverdraft());
        config.setOverdraftLimit(request.getOverdraftLimit());
        config.setMaxAccountsPerClient(request.getMaxAccountsPerClient());
        config.setDefault(request.isDefault());
        config.setEligibilityCriteria(request.getEligibilityCriteria());
        config.setBenefits(request.getBenefits());
        config.setRequiredDocuments(request.getRequiredDocuments());
        config.setUpdatedBy(updatedBy);
        config.setUpdatedAt(LocalDateTime.now());
        
        config = typeConfigRepository.save(config);
        
        return mapToResponse(config);
    }
    
    @Transactional
    public void deleteAccountType(String id) {
        log.info("du type de compte: {}", id);
        
        AccountTypeConfiguration config = typeConfigRepository.findById(id)
                .orElseThrow(() -> new AccountTypeNotFoundException("Type de compte non trouvé: " + id));
        
        typeConfigRepository.delete(config);
        log.info("de compte supprimé: {}", id);
    }
    
    @Transactional(readOnly = true)
    public List<AccountTypeConfigurationResponse> getAllAccountTypes() {
        return typeConfigRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<AccountTypeConfigurationResponse> getActiveAccountTypes() {
        return typeConfigRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public AccountTypeConfigurationResponse getAccountType(String id) {
        AccountTypeConfiguration config = typeConfigRepository.findById(id)
                .orElseThrow(() -> new AccountTypeNotFoundException("Type de compte non trouvé: " + id));
        return mapToResponse(config);
    }
    
    @Transactional(readOnly = true)
    public AccountTypeConfigurationResponse getAccountTypeByCode(String code) {
        AccountTypeConfiguration config = typeConfigRepository.findByCode(code)
                .orElseThrow(() -> new AccountTypeNotFoundException("Type de compte non trouvé avec le code: " + code));
        return mapToResponse(config);
    }
    
   @Transactional(readOnly = true)
public AccountTypeConfigurationResponse getAccountTypeByType(String accountTypeStr) {
    try {
        AccountType accountType = AccountType.valueOf(accountTypeStr.toUpperCase());
        List<AccountTypeConfiguration> configs = typeConfigRepository.findByAccountTypeAndActiveTrue(accountType);
        if (configs.isEmpty()) {
            throw new AccountTypeNotFoundException("Aucune configuration trouvée pour le type: " + accountTypeStr);
        }
        return mapToResponse(configs.get(0));
    } catch (IllegalArgumentException e) {
        throw new AccountTypeNotFoundException("Type de compte invalide: " + accountTypeStr);
    }
}
    
    @Transactional
    public AccountTypeConfigurationResponse toggleActive(String id) {
        AccountTypeConfiguration config = typeConfigRepository.findById(id)
                .orElseThrow(() -> new AccountTypeNotFoundException("Type de compte non trouvé: " + id));
        
        config.setActive(!config.isActive());
        config = typeConfigRepository.save(config);
        
        log.info("de compte {}: {}", config.getName(), config.isActive() ? "activé" : "désactivé");
        
        return mapToResponse(config);
    }
    
   @Transactional(readOnly = true)
public boolean existsByNameAndType(String accountName, String accountTypeStr) {
    try {
        AccountType accountType = AccountType.valueOf(accountTypeStr.toUpperCase());
        return typeConfigRepository.existsByNameAndAccountType(accountName, accountType);
    } catch (IllegalArgumentException e) {
        return false;
    }
}
    
    @Transactional(readOnly = true)
public String getValidNamesForType(String accountTypeStr) {
    try {
        AccountType accountType = AccountType.valueOf(accountTypeStr.toUpperCase());
        List<AccountTypeConfiguration> configs = typeConfigRepository.findByAccountTypeAndActiveTrue(accountType);
        return configs.stream()
                .map(AccountTypeConfiguration::getName)
                .collect(Collectors.joining(", "));
    } catch (IllegalArgumentException e) {
        return "";
    }
}
    
    public AccountTypeConfigurationResponse mapToResponse(AccountTypeConfiguration config) {
        AccountCategoryResponse categoryResponse = AccountCategoryResponse.builder()
                .id(config.getCategory().getId())
                .code(config.getCategory().getCode())
                .name(config.getCategory().getName())
                .build();
        
        return AccountTypeConfigurationResponse.builder()
                .id(config.getId())
                .code(config.getCode())
                .accountType(config.getAccountType())
                .category(categoryResponse)
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
}