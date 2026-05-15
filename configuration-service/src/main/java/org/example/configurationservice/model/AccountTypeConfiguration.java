package org.example.configurationservice.model;

import org.example.configurationservice.model.enums.AccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_type_configurations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTypeConfiguration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true, nullable = false)
    private String code;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private AccountCategory category;
    
    @Column(nullable = false)
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
    
    @Column(columnDefinition = "TEXT")
    private String eligibilityCriteria;
    
    @Column(columnDefinition = "TEXT")
    private String benefits;
    
    private String requiredDocuments;
    
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        active = true;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}