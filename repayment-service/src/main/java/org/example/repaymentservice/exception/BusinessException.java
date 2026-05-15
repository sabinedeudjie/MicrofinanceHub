package org.example.repaymentservice.exception;

import java.math.BigDecimal;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    
    private final String code;
    
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
    
    //  methods pour les erreurs courantes
    public static BusinessException loanAlreadyPaid() {
        return new BusinessException("LOAN_ALREADY_PAID", "Ce prêt est déjà entièrement remboursé. Aucun paiement supplémentaire n'est accepté.");
    }
    
    public static BusinessException invalidAmount(java.math.BigDecimal expected, java.math.BigDecimal received) {
        return new BusinessException("INVALID_AMOUNT", "Le montant doit être de " + expected + " FCFA (reçu: " + received + ")");
    }
    
    public static BusinessException noScheduleFound() {
        return new BusinessException("NO_SCHEDULE", "Aucun plan de remboursement trouvé pour ce prêt");
    }
    
    public static BusinessException noUnpaidInstallment() {
        return new BusinessException("NO_UNPAID_INSTALLMENT", "Aucune échéance impayée pour ce prêt");
    }
    
    public static BusinessException loanNotFound(String loanId) {
        return new BusinessException("LOAN_NOT_FOUND", "Prêt non trouvé: " + loanId);
    }
    
    public static BusinessException paymentFailed(String reason) {
        return new BusinessException("PAYMENT_FAILED", "Le paiement a échoué: " + reason);
    }

    public static BusinessException loanNotActive(String currentStatus) {
        return new BusinessException("LOAN_NOT_ACTIVE", 
           "Ce prêt n'est pas encore actif. Statut actuel: " + currentStatus + 
           ". Les remboursements ne sont possibles que pour les prêts actifs.");
    }

    public static BusinessException partialPaymentNotAllowed(BigDecimal dueAmount, BigDecimal paidAmount) {
        return new BusinessException("PARTIAL_PAYMENT_NOT_ALLOWED", 
        "Les paiements partiels ne sont pas acceptés. Montant dû: " + dueAmount + 
        " FCFA, Montant fourni: " + paidAmount + " FCFA");
    }
    
    public static BusinessException unauthorized(String message) {
       return new BusinessException("UNAUTHORIZED", message);
    }

    public static BusinessException insufficientFunds(String message) {
        return new BusinessException("INSUFFICIENT_FUNDS", message != null ? message : "Solde insuffisant pour effectuer cette transaction");
    }

    //  BusinessException.java
    public static BusinessException invalidClientForLoan(String clientId, String loanId) {
        return new BusinessException("INVALID_CLIENT_FOR_LOAN", 
        "Le client " + clientId + " n'est pas le propriétaire du prêt " + loanId);
    }
    
}

//  org.example.repaymentservice.exception;

//  class BusinessException extends RuntimeException {
//      final String code;
//      final String message;
    
//      BusinessException(String code, String message) {
//         (message);
//         .code = code;
//         .message = message;
//     
    
//      String getCode() { return code; }
//      String getMessage() { return message; }
// 