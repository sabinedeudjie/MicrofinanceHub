package org.example.loanservice.dto.equest;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoanRejectionRequest {
    
    @NotBlank(message = "La raison du rejet est requise")
    private String rejectionReason;

    private String comments;
}