package org.example.configurationservice.dto.reponse;

import org.example.configurationservice.model.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTypeConfigurationResponse {
    
    private String id;
    private String code;
    
    private AccountType accountType;
    
    private AccountCategoryResponse category;
    private String name;
    private String description;
    private BigDecimal minimumBalance;
    private BigDecimal maximumBalance;
    private BigDecimal interestRate;
    private BigDecimal monthlyFee;
    private BigDecimal transactionFee;
    private boolean allowOverdraft;
    private BigDecimal overdraftLimit;
    private boolean active;
    private int maxAccountsPerClient;
    private boolean isDefault;
    private String eligibilityCriteria;
    private String benefits;
    private String requiredDocuments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}