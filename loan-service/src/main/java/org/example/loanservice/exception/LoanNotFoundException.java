package org.example.loanservice.exception;

public class LoanNotFoundException extends RuntimeException {
    public LoanNotFoundException(String id) {
        super("Prêt non trouvé avec l'ID: " + id);
    }
}