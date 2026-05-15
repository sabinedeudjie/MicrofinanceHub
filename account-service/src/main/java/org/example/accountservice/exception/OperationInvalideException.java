package org.example.accountservice.exception;

/**
 * Exception levée quand une opération métier est impossible.
 * Ex : solde insuffisant, compte bloqué, plafond dépassé.
 * Provoque une réponse HTTP 400.
 */
public class OperationInvalideException extends RuntimeException {
    public OperationInvalideException(String message) {
        super(message);
    }
}
