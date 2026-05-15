package org.example.repaymentservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

import org.example.repaymentservice.model.enums.PaymentMethod;

@Data
public class PaymentRequest {
    
    @NotBlank(message = "L'ID du prêt est requis")
    private String loanId;
    
    @NotNull(message = "Le montant est requis")
    @DecimalMin(value = "1000", message = "Le montant minimum est de 1000 FCFA")
    private BigDecimal amount;
    
    @NotNull(message = "La méthode de paiement est requise")
    private PaymentMethod paymentMethod;
    
    private String transactionId;

    // Numéro de téléphone Mobile Money (obligatoire si paymentMethod == MOBILE_MONEY)
    private String numeroPaiement;

    // ID du compte bancaire source (obligatoire si paymentMethod == BANK_TRANSFER)
    private Long compteSourceId;

    private String notes;
    private String registeredBy;
}