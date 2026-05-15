package org.example.loanservice.exception;

public class AccountNotFoundException extends RuntimeException {
    
    public AccountNotFoundException(String clientId) {
        super("Le client " + clientId + " n'a pas de compte actif. Veuillez d'abord ouvrir un compte.");
    }
    
    public AccountNotFoundException(String clientId, String message) {
        super(message);
    }
}