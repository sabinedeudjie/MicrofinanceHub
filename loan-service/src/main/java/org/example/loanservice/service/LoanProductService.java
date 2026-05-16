package org.example.loanservice.service;

import org.example.loanservice.dto.equest.LoanProductRequest;
import org.example.loanservice.dto.response.LoanProductResponse;
import org.example.loanservice.exception.LoanProductNotFoundException;
import org.example.loanservice.model.LoanProduct;
import org.example.loanservice.repository.LoanProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanProductService {
    
    private final LoanProductRepository loanProductRepository;
    
    @Transactional
    public LoanProductResponse createProduct(LoanProductRequest request, String createdBy) {
        log.info("d'un nouveau produit de prêt: {} par {}", createdBy);
        
        
        //  les montants
        if (request.getMinAmount().compareTo(request.getMaxAmount()) > 0) {
            throw new RuntimeException("Le montant minimum ne peut pas être supérieur au montant maximum");
        }
        
        //  les durées
        if (request.getMinTermMonths() > request.getMaxTermMonths()) {
            throw new RuntimeException("La durée minimum ne peut pas être supérieure à la durée maximum");
        }
        
        LoanProduct product = LoanProduct.builder()
            .name(request.getName())
            .description(request.getDescription())
            .minAmount(request.getMinAmount())
            .maxAmount(request.getMaxAmount())
            .minTermMonths(request.getMinTermMonths())
            .maxTermMonths(request.getMaxTermMonths())
            .interestRate(request.getInterestRate())
            .active(true)
            .build();
        
        product = loanProductRepository.save(product);
        
        log.info("créé avec succès: {} - {}", product.getName());
        
        return mapToResponse(product);
    }
    
    @Transactional
    public LoanProductResponse updateProduct(String id, LoanProductRequest request, String updatedBy) {
        log.info("à jour du produit: {} par {}", id, updatedBy);
        
        LoanProduct product = loanProductRepository.findById(id)
            .orElseThrow(() -> new LoanProductNotFoundException(id));
        
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setMinAmount(request.getMinAmount());
        product.setMaxAmount(request.getMaxAmount());
        product.setMinTermMonths(request.getMinTermMonths());
        product.setMaxTermMonths(request.getMaxTermMonths());
        product.setInterestRate(request.getInterestRate());
        
        product = loanProductRepository.save(product);
        
        return mapToResponse(product);
    }
    
    @Transactional
    public LoanProductResponse toggleProductStatus(String id, boolean active, String updatedBy) {
        log.info("de statut du produit {}: {} par {}", id, active ? "ACTIF" : "INACTIF", updatedBy);
        
        LoanProduct product = loanProductRepository.findById(id)
            .orElseThrow(() -> new LoanProductNotFoundException(id));
        
        product.setActive(active);
        //.setUpdatedBy(updatedBy);
        
        product = loanProductRepository.save(product);
        
        return mapToResponse(product);
    }
    
    @Transactional(readOnly = true)
    public LoanProductResponse getProduct(String id) {
        LoanProduct product = loanProductRepository.findById(id)
            .orElseThrow(() -> new LoanProductNotFoundException(id));
        return mapToResponse(product);
    }
    
    
    @Transactional(readOnly = true)
    public List<LoanProductResponse> getAllActiveProducts() {
        return loanProductRepository.findByActiveTrue().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Page<LoanProductResponse> getActiveProducts(Pageable pageable) {
        return loanProductRepository.findByActiveTrue(pageable)
            .map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<LoanProductResponse> getAllProducts(Pageable pageable) {
        return loanProductRepository.findAll(pageable)
            .map(this::mapToResponse);
    }
    
    @Transactional
    public void deleteProduct(String id) {
        log.info("du produit: {}", id);
        LoanProduct product = loanProductRepository.findById(id)
            .orElseThrow(() -> new LoanProductNotFoundException(id));
        loanProductRepository.delete(product);
    }

    public LoanProduct getProductEntity(String productId) {
        if (productId == null || productId.isEmpty()) {
            return null;
        }
        return loanProductRepository.findById(productId).orElse(null);
    }

    public LoanProduct getProductEntityOrThrow(String productId) {
        return loanProductRepository.findById(productId)
            .orElseThrow(() -> new LoanProductNotFoundException(productId));
    }
    
    private LoanProductResponse mapToResponse(LoanProduct product) {
        return LoanProductResponse.builder()
            .id(product.getId())
            .name(product.getName())
            .description(product.getDescription())
            .minAmount(product.getMinAmount())
            .maxAmount(product.getMaxAmount())
            .minTermMonths(product.getMinTermMonths())
            .maxTermMonths(product.getMaxTermMonths())
            .interestRate(product.getInterestRate())
            .active(product.isActive())
            .build();
    }
}