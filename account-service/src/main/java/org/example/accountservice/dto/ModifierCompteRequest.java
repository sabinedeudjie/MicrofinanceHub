package org.example.accountservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifierCompteRequest {

    @Size(max = 255)
    private String description;

    @DecimalMin(value = "0.0", message = "Le solde minimum doit être positif ou nul")
    private BigDecimal soldeMinimum;

    @DecimalMin(value = "0.0", message = "Le plafond doit être positif ou nul")
    private BigDecimal plafond;

    @DecimalMin(value = "0.0", message = "Le taux d'intérêt doit être positif ou nul")
    @DecimalMax(value = "1.0", message = "Le taux d'intérêt ne peut pas dépasser 100%")
    private BigDecimal tauxInteret;
}