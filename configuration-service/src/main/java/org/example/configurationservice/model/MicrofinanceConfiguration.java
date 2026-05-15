package org.example.configurationservice.model;

import org.example.configurationservice.model.enums.ClientIdGenerationStrategy;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "microfinance_configurations")
public class MicrofinanceConfiguration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true, nullable = false)
    private String microfinanceCode;
    
    private String affiliatedBankCode;
    private boolean affiliatedToBank;
    private String agencyCode;
    private String agencyName;
    private String agencyAddress;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "client_id_strategy")
    private ClientIdGenerationStrategy clientIdStrategy;
    
    private String customClientIdPattern;
    private boolean enableRibGeneration;
    private String bankCode;
    private String countryCode;
    private String controlKeyAlgorithm;
    
    
    private boolean useCustomFormat;
    private String accountNumberFormat;
    private Integer bankCodeLength;
    private Integer accountNumberLength;
    private Integer controlKeyLength;
    private String separator;
    private String fixedPrefix;
    private String fixedSuffix;
    private String generationStrategy;
    private boolean includeCheckDigit;
    
    private boolean active;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        active = true;
        if (clientIdStrategy == null) {
            clientIdStrategy = ClientIdGenerationStrategy.SEQUENTIAL;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}