package org.example.configurationservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AccountNumberFormatRequest {
    
    private boolean useCustomFormat = false;
    
    @Pattern(regexp = "^[A-Za-z0-9\\-\\_\\{\\}]+$", 
             message = "Le format ne peut contenir que des lettres, chiffres, tirets, underscores et accolades")
    private String accountNumberFormat;  //  "{BANK}-{AGENCY}-{ACCOUNT}-{KEY}"
    
    @Min(value = 1, message = "La longueur du code banque doit être au moins 1")
    private Integer bankCodeLength = 5;
    
    @Min(value = 5, message = "La longueur du numéro de compte doit être au moins 5")
    private Integer accountNumberLength = 11;
    
    @Min(value = 1, message = "La longueur de la clé de contrôle doit être au moins 1")
    private Integer controlKeyLength = 2;
    
    private String separator = "-";
    
    private String fixedPrefix;
    
    private String fixedSuffix;
    
    @Pattern(regexp = "SEQUENTIAL|RANDOM|TIMESTAMP|CUSTOM", 
             message = "Stratégie invalide. Utilisez: SEQUENTIAL, RANDOM, TIMESTAMP, CUSTOM")
    private String generationStrategy = "SEQUENTIAL";
    
    private boolean includeCheckDigit = true;
}