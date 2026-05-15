package org.example.transactionservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.example.transactionservice.model.ModePaiement;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepotRequest {

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "1.0", message = "Le montant doit être supérieur à 0")
    private BigDecimal montant;

    @NotNull(message = "Le mode de dépôt est obligatoire")
    private ModePaiement modeDepot;

    @Pattern(
        regexp = "^237[6-9][0-9]{8}$",
        message = "Numéro Mobile Money invalide — format attendu : 237 suivi de 9 chiffres (ex: 237677777777)"
    )
    private String numeroPaiement;

    private String description;
}
