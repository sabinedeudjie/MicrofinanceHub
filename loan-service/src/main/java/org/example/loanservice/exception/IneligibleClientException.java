package org.example.loanservice.exception;

public class IneligibleClientException extends RuntimeException {
    public IneligibleClientException(String message) {
        super(message);
    }
}