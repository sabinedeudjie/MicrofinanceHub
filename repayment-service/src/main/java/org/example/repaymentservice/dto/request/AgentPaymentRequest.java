package org.example.repaymentservice.dto.request;

import org.example.repaymentservice.model.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AgentPaymentRequest {
    
    @NotBlank(message = "L'ID du prêt est requis")
    private String loanId;
    
    @NotBlank(message = "L'ID du client est requis")
    private String clientId;
    
    @NotNull(message = "Le montant est requis")
    @DecimalMin(value = "1000", message = "Le montant minimum est de 1000 FCFA")
    private BigDecimal amount;
    
    @NotNull(message = "La méthode de paiement est requise")
    private PaymentMethod paymentMethod;
    
    private String transactionId;
    private String numeroPaiement; // Numéro Mobile Money si mode MOBILE_MONEY
    private Long compteSourceId;   // ID du compte source si mode BANK_TRANSFER
    private String notes;
    private String receiptNumber;
}