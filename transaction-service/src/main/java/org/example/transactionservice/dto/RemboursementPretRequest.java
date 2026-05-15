package org.example.transactionservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.transactionservice.model.ModePaiement;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemboursementPretRequest {

    @NotBlank(message = "L'ID du prêt est obligatoire")
    private String loanId;

    @NotBlank(message = "L'ID du client est obligatoire")
    private String clientId;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "1.0", message = "Le montant doit être supérieur à 0")
    private BigDecimal montant;

    @NotNull(message = "Le mode de paiement est obligatoire")
    private ModePaiement modePaiement;

    // Obligatoire pour MOBILE_MONEY — format: 237XXXXXXXXX
    private String numeroPaiement;

    // ID du compte source pour VIREMENT_BANCAIRE
    private Long compteSourceId;

    private String description;

    // Référence du payment côté repayment-service pour corrélation webhook
    private String referenceRepayment;
}
