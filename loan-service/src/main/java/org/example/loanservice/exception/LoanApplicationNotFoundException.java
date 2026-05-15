package org.example.loanservice.exception;

public class LoanApplicationNotFoundException extends RuntimeException {
    public LoanApplicationNotFoundException(String id) {
        super("Demande de prêt non trouvée avec l'ID: " + id);
    }
}