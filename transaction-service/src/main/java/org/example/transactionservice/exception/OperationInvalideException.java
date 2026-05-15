package org.example.transactionservice.exception;

import lombok.Getter;

@Getter
public class OperationInvalideException extends RuntimeException {
    private String errorCode;

    public OperationInvalideException(String message) {
        super(message);
    }

    public OperationInvalideException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
