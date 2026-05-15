package org.example.transactionservice.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(Long id) {
        super("Transaction introuvable : " + id);
    }

    public TransactionNotFoundException(String reference) {
        super("Transaction introuvable (réf) : " + reference);
    }
}
