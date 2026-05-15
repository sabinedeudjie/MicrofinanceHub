package org.example.loanservice.exception;

public class LoanAlreadyProcessedException extends RuntimeException {
    
    private final String applicationId;
    private final String currentStatus;
    
    public LoanAlreadyProcessedException(String applicationId, String currentStatus) {
        super(String.format("La demande %s a déjà été traitée. Statut actuel: %s", applicationId, currentStatus));
        this.applicationId = applicationId;
        this.currentStatus = currentStatus;
    }
    
    public String getApplicationId() { return applicationId; }
    public String getCurrentStatus() { return currentStatus; }
}