package org.example.configurationservice.dto.reponse;

import org.example.configurationservice.model.enums.ClientIdGenerationStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MicrofinanceConfigurationResponse {
    
    private String id;
    private String microfinanceCode;
    private String affiliatedBankCode;
    private boolean affiliatedToBank;
    private String agencyCode;
    private String agencyName;
    private String agencyAddress;
    
    
    private ClientIdGenerationStrategy clientIdStrategy;
    
    private String customClientIdPattern;
    private boolean enableRibGeneration;
    private String bankCode;
    private String countryCode;
    private String controlKeyAlgorithm;
    
    //  de numéro de compte
    private boolean useCustomFormat;
    private String accountNumberFormat;
    private Integer bankCodeLength;
    private Integer agencyCodeLength;
    private Integer accountNumberLength;
    private Integer controlKeyLength;
    private String separator;
    private String fixedPrefix;
    private String fixedSuffix;
    private String generationStrategy;
    private boolean includeCheckDigit;
    
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}