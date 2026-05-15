package org.example.transactionservice.campay;

public class CamPayException extends RuntimeException {
    public CamPayException(String message) {
        super(message);
    }

    public CamPayException(String message, Throwable cause) {
        super(message, cause);
    }
}
