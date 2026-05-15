package org.example.configurationservice.dto.request;

import org.example.configurationservice.model.enums.ClientIdGenerationStrategy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MicrofinanceConfigurationRequest {
    
    @NotBlank(message = "Le code microfinance est requis")
    @Pattern(regexp = "^[0-9]{5}$", message = "Le code doit contenir exactement 5 chiffres")
    private String microfinanceCode;
    
    @Pattern(regexp = "^[0-9]{5}$", message = "Le code banque doit contenir exactement 5 chiffres")
    private String affiliatedBankCode;
    
    @NotNull(message = "Le statut d'affiliation est requis")
    private boolean affiliatedToBank;
    
    private ClientIdGenerationStrategy clientIdStrategy;
    
    private String customClientIdPattern;
    private boolean enableRibGeneration = true;
    
    @Pattern(regexp = "^[0-9]{5}$", message = "Le code banque pour RIB doit contenir exactement 5 chiffres")
    private String bankCode;
    
    @Pattern(regexp = "^[A-Z]{2}$", message = "Le code pays doit contenir 2 lettres majuscules")
    private String countryCode = "CM";
    
    private String controlKeyAlgorithm = "MOD97";
}