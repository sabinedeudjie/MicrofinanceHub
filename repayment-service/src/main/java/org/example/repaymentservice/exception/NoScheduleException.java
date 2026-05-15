package org.example.repaymentservice.exception;

public class NoScheduleException extends BusinessException {
    public NoScheduleException(String loanId) {
        super("NO_SCHEDULE", "Aucune échéance trouvée pour le prêt: " + loanId);
    }
}
