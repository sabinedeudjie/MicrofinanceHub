package org.example.loanservice.dto.equest;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApprovalRequest {
    
    @NotNull(message = "Le montant approuvé est requis")
    @DecimalMin(value = "50000", message = "Le montant minimum est de 50 000 FCFA")
    private BigDecimal approvedAmount;
    
    @NotNull(message = "La durée approuvée est requise")
    @Min(value = 1, message = "La durée minimum est de 1 mois")
    private Integer approvedTermMonths;
    
    @NotNull(message = "Le taux d'intérêt est requis")
    @DecimalMin(value = "0.1", message = "Le taux d'intérêt minimum est de 0.1%")
    private BigDecimal interestRate;
    
    private String approvalNotes;

    private String productId;
    //  pour le rejet
   //  String rejectionReason;
}