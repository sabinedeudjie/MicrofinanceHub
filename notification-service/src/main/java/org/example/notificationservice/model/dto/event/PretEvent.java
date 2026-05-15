package org.example.notificationservice.model.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

//  avec LoanApprovedEvent, LoanRejectedEvent, LoanDisbursedEvent de loan-service
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PretEvent {
    private String    loanId;
    private String    loanNumber;
    private String    applicationId;
    private String    applicationNumber;
    private String    clientId;
    private String    clientEmail;
    private String    clientFirstName;
    private String    clientLastName;
    private BigDecimal amount;
    private BigDecimal monthlyPayment;
    private Integer   termMonths;
    private BigDecimal interestRate;
    private String    reason;            //  de rejet (LoanRejectedEvent)
    private LocalDateTime disbursementDate;
    private LocalDateTime nextPaymentDate;
    private LocalDateTime timestamp;

    //  pour le consumer
    public String getClientNom() {
        return (clientFirstName != null ? clientFirstName : "") + " " + (clientLastName != null ? clientLastName : "");
    }
}
