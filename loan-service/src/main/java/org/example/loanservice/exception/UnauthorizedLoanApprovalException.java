package org.example.loanservice.exception;

public class UnauthorizedLoanApprovalException extends RuntimeException {
    
    public UnauthorizedLoanApprovalException(String message) {
        super(message);
    }
}