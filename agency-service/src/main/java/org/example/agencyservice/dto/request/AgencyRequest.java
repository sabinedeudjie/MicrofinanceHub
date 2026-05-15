package org.example.agencyservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AgencyRequest {
    
    @NotBlank(message = "Le code de l'agence est requis")
    @Pattern(regexp = "^[0-9]{5}$", message = "Le code doit contenir exactement 5 chiffres")
    private String code;
    
    @NotBlank(message = "Le nom de l'agence est requis")
    private String name;
    
    private String address;
    private String city;
    private String phoneNumber;
    private String email;
    private String region;
    
    @NotBlank(message = "L'ID du directeur est requis")
    private String directorId;
    
    private String directorEmail;
    private String directorName;
}