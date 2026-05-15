package org.example.accountservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.example.accountservice.model.TypeCompte;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OuvrirCompteRequest {

    @NotNull(message = "L'identifiant client est obligatoire")
    private String clientId;

    @NotNull(message = "Le type de compte est obligatoire")
    private TypeCompte typeCompte;

    @DecimalMin(value = "0.0", message = "Le solde initial doit être positif ou nul")
    private BigDecimal soldeInitial;

    @Size(max = 255)
    private String description;

    /** Fourni par le frontend pour que la notification puisse être envoyée sans appel HTTP supplémentaire */
    private String clientEmail;
    private String clientNom;
}
