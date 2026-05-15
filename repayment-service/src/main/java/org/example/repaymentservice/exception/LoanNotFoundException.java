package org.example.repaymentservice.exception;

public class LoanNotFoundException extends BusinessException {
    public LoanNotFoundException(String loanId) {
        super("LOAN_NOT_FOUND", "Prêt non trouvé: " + loanId);
    }
}