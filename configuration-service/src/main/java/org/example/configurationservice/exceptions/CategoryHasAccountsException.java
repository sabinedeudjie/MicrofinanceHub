package org.example.configurationservice.exceptions;

public class CategoryHasAccountsException extends RuntimeException {
    
    public CategoryHasAccountsException(String message) {
        super(message);
    }
    
    public CategoryHasAccountsException(String message, Throwable cause) {
        super(message, cause);
    }
}