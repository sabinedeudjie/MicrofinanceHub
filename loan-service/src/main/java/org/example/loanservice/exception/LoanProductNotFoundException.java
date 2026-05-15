package org.example.loanservice.exception;

public class LoanProductNotFoundException extends RuntimeException {
    
    public LoanProductNotFoundException(String id) {
        super("Produit de prêt non trouvé avec l'ID: " + id);
    }
    
    public LoanProductNotFoundException(String id, String message) {
        super(message);
    }
}