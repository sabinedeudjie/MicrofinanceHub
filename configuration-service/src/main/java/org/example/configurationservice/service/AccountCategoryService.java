package org.example.configurationservice.service;

import org.example.configurationservice.dto.reponse.AccountCategoryResponse;
import org.example.configurationservice.dto.reponse.AccountTypeConfigurationResponse;
import org.example.configurationservice.dto.request.AccountCategoryRequest;
import org.example.configurationservice.exceptions.CategoryHasAccountsException;
import org.example.configurationservice.exceptions.CategoryNotFoundException;
import org.example.configurationservice.exceptions.DuplicateCodeException;
import org.example.configurationservice.model.AccountCategory;
import org.example.configurationservice.repository.AccountCategoryRepository;
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
public class AccountCategoryService {
    
    private final AccountCategoryRepository categoryRepository;
    private final AccountTypeConfigurationService accountTypeService;
    
    @Transactional
    public AccountCategoryResponse createCategory(AccountCategoryRequest request, String createdBy) {
        log.info("de la catégorie: {}", request.getCode());
        
        if (categoryRepository.existsByCode(request.getCode())) {
            throw new DuplicateCodeException("Une catégorie existe déjà avec le code: " + request.getCode());
        }
        
        AccountCategory category = AccountCategory.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .icon(request.getIcon() != null ? request.getIcon() : "🏦")
                .color(request.getColor() != null ? request.getColor() : "#4CAF50")
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .createdBy(createdBy)
                .build();
        
        category = categoryRepository.save(category);
        log.info("créée: {}", category.getName());
        
        return mapToResponse(category);
    }
    
    @Transactional
    public AccountCategoryResponse updateCategory(String id, AccountCategoryRequest request, String updatedBy) {
        log.info("à jour de la catégorie: {}", id);
        
        AccountCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Catégorie non trouvée: " + id));
        
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        category.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : category.getDisplayOrder());
        category.setUpdatedBy(updatedBy);
        category.setUpdatedAt(LocalDateTime.now());
        
        category = categoryRepository.save(category);
        
        return mapToResponse(category);
    }
    
    @Transactional
    public void deleteCategory(String id) {
        log.info("de la catégorie: {}", id);
        
        AccountCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Catégorie non trouvée: " + id));
        
        if (category.getAccountTypes() != null && !category.getAccountTypes().isEmpty()) {
            int count = category.getAccountTypes().size();
            throw new CategoryHasAccountsException(
                String.format("Impossible de supprimer la catégorie '%s' car elle contient %d type(s) de compte(s). Supprimez d'abord les types de comptes associés.",
                    category.getName(), count)
            );
        }
        
        try {
            categoryRepository.delete(category);
            log.info("supprimée: {}", id);
        } catch (DataIntegrityViolationException e) {
            throw new CategoryHasAccountsException(
                "Impossible de supprimer cette catégorie car elle est référencée par des types de comptes."
            );
        }
    }
    
    @Transactional(readOnly = true)
    public List<AccountCategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<AccountCategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public AccountCategoryResponse getCategory(String id) {
        AccountCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Catégorie non trouvée: " + id));
        return mapToResponse(category);
    }
    
    @Transactional
    public AccountCategoryResponse toggleActive(String id) {
        AccountCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Catégorie non trouvée: " + id));
        
        category.setActive(!category.isActive());
        category = categoryRepository.save(category);
        
        log.info("{}: {}", category.getName(), category.isActive() ? "activée" : "désactivée");
        
        return mapToResponse(category);
    }
    
    private AccountCategoryResponse mapToResponse(AccountCategory category) {
        List<AccountTypeConfigurationResponse> accountTypes = null;
        if (category.getAccountTypes() != null && !category.getAccountTypes().isEmpty()) {
            accountTypes = category.getAccountTypes().stream()
                    .filter(type -> type.isActive())
                    .map(accountTypeService::mapToResponse)
                    .collect(Collectors.toList());
        }
        
        return AccountCategoryResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .description(category.getDescription())
                .icon(category.getIcon())
                .color(category.getColor())
                .displayOrder(category.getDisplayOrder())
                .active(category.isActive())
                .accountTypes(accountTypes)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}