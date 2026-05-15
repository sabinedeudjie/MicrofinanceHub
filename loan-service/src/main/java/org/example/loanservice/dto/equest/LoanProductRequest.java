package org.example.loanservice.dto.equest;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanProductRequest {
    
    
    @NotBlank(message = "Le nom du produit est requis")
    @Size(min = 3, max = 100, message = "Le nom doit contenir entre 3 et 100 caractères")
    private String name;
    
    private String description;
    
    @NotNull(message = "Le montant minimum est requis")
    @DecimalMin(value = "10000", message = "Le montant minimum doit être d'au moins 10 000 FCFA")
    private BigDecimal minAmount;
    
    @NotNull(message = "Le montant maximum est requis")
    @DecimalMin(value = "10000", message = "Le montant maximum doit être d'au moins 10 000 FCFA")
    private BigDecimal maxAmount;
    
    @NotNull(message = "La durée minimum est requise")
    @Min(value = 1, message = "La durée minimum doit être d'au moins 1 mois")
    private Integer minTermMonths;
    
    @NotNull(message = "La durée maximum est requise")
    @Min(value = 1, message = "La durée maximum doit être d'au moins 1 mois")
    private Integer maxTermMonths;
    
    @NotNull(message = "Le taux d'intérêt est requis")
    @DecimalMin(value = "0", message = "Le taux d'intérêt doit être positif")
    @DecimalMax(value = "100", message = "Le taux d'intérêt ne peut pas dépasser 100%")
    private BigDecimal interestRate;

}