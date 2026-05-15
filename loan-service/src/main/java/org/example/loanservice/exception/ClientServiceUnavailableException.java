package org.example.loanservice.exception;

public class ClientServiceUnavailableException extends RuntimeException {
    public ClientServiceUnavailableException(String message) {
        super(message);
    }
}