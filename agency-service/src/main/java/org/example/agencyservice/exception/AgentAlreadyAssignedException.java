package org.example.agencyservice.exception;

public class AgentAlreadyAssignedException extends RuntimeException {
    public AgentAlreadyAssignedException(String message) {
        super(message);
    }
}