package org.example.loanservice.dto.equest;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {
    
    @NotBlank(message = "L'ID du client est requis")
    private String clientId;
    
    
    @NotBlank(message = "Le numéro de compte est requis")
    private String accountNumber;
    
    @NotNull(message = "Le montant est requis")
    @DecimalMin(value = "50000", message = "Le montant minimum est de 50 000 FCFA")
    private BigDecimal requestedAmount;
    
    @NotNull(message = "La durée est requise")
    @Min(value = 1, message = "La durée minimum est de 1 mois")
    private Integer termMonths;
    
    private String purpose;
    
    private BigDecimal monthlyIncome;
    
    private String employmentStatus;
}


//  org.example.loanservice.dto.equest;

//  jakarta.validation.constraints.DecimalMin;
//  jakarta.validation.constraints.Min;
//  jakarta.validation.constraints.NotBlank;
//  jakarta.validation.constraints.NotNull;
//  lombok.Data;

//  java.math.BigDecimal;

// 
//  class LoanApplicationRequest {
    
//     (message = "L'ID du client est requis")
//      String clientId;
    
//     (message = "Le montant est requis")
//     (value = "50000", message = "Le montant minimum est de 50 000 FCFA")
//      BigDecimal requestedAmount;
    
//     (message = "La durée est requise")
//     (value = 1, message = "La durée minimum est de 1 mois")
//      Integer termMonths;
    
//      String purpose;
    
//      BigDecimal monthlyIncome;
    
//      String employmentStatus;
// 