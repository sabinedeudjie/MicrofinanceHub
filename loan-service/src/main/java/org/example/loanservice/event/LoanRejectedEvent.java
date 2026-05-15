package org.example.loanservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRejectedEvent {
    
   private String applicationId;
    private String applicationNumber;
    private String clientId;
    private String clientEmail;
    private String clientFirstName;
    private String clientLastName;
    private String reason;
    private LocalDateTime timestamp;
    @Builder.Default
    private String eventType = "LOAN_REJECTED";
}