package org.example.configurationservice.exceptions;

public class AccountTypeNotFoundException extends RuntimeException {
    
    public AccountTypeNotFoundException(String message) {
        super(message);
    }
    
    public AccountTypeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}