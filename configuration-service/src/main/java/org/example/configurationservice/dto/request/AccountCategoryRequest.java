package org.example.configurationservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AccountCategoryRequest {
    
    @NotBlank(message = "Le code est requis")
    private String code;
    
    @NotBlank(message = "Le nom est requis")
    private String name;
    
    private String description;
    private String icon;
    private String color;
    
    @NotNull(message = "L'ordre d'affichage est requis")
    private Integer displayOrder;
}