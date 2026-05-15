package org.example.loanservice.exception;

public class LoanNotApprovedException extends RuntimeException {
    public LoanNotApprovedException(String id) {
        super("Le prêt n'est pas approuvé, impossible de décaisser: " + id);
    }
}