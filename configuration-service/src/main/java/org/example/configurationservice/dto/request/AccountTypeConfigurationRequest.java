package org.example.configurationservice.dto.request;

import org.example.configurationservice.model.enums.AccountType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AccountTypeConfigurationRequest {
    
    @NotBlank(message = "Le code est requis")
    private String code;
    
    @NotBlank(message = "L'ID de la catégorie est requis")
    private String categoryId;
    
    @NotNull(message = "Le type de compte est requis")
    private AccountType accountType;
    
    @NotBlank(message = "Le nom est requis")
    private String name;
    
    private String description;
    
    @NotNull(message = "Le solde minimum est requis")
    @DecimalMin(value = "0", message = "Le solde minimum doit être >= 0")
    private BigDecimal minimumBalance;
    
    @NotNull(message = "Le solde maximum est requis")
    @DecimalMin(value = "-1", message = "Le solde maximum doit être >= -1")
    private BigDecimal maximumBalance;
    
    @NotNull(message = "Le taux d'intérêt est requis")
    @DecimalMin(value = "0", message = "Le taux d'intérêt doit être >= 0")
    @DecimalMax(value = "100", message = "Le taux d'intérêt doit être <= 100")
    private BigDecimal interestRate;
    
    @NotNull(message = "Les frais mensuels sont requis")
    @DecimalMin(value = "0", message = "Les frais mensuels doivent être >= 0")
    private BigDecimal monthlyFee;
    
    @NotNull(message = "Les frais de transaction sont requis")
    @DecimalMin(value = "0", message = "Les frais de transaction doivent être >= 0")
    private BigDecimal transactionFee;
    
    private boolean allowOverdraft;
    
    @DecimalMin(value = "0", message = "La limite de découvert doit être >= 0")
    private BigDecimal overdraftLimit;
    
    @Min(value = 1, message = "Le nombre maximum de comptes doit être >= 1")
    private int maxAccountsPerClient = 5;
    
    private boolean isDefault = false;
    
    private String eligibilityCriteria;
    private String benefits;
    private String requiredDocuments;
}