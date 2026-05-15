package org.example.repaymentservice.dto.response;

import org.example.repaymentservice.model.enums.PaymentMethod;
import org.example.repaymentservice.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    
    private String id;
    private String paymentNumber;
    private String loanId;
    private String clientId;
    private String clientName;
    private BigDecimal amount;
    private BigDecimal penaltyAmount;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String receiptNumber;
    private LocalDateTime paymentDate;
    private Integer remainingInstallments;
    private BigDecimal remainingBalance;
    private String message;
    private BigDecimal overpayment;
    private List<Integer> paidInstallments;
    private String registeredBy;
    private String notes;          
    private String paidBy;     
    
}


//  org.example.repaymentservice.dto.response;

//  lombok.AllArgsConstructor;
//  lombok.Builder;
//  lombok.Data;
//  lombok.NoArgsConstructor;

//  java.math.BigDecimal;
//  java.time.LocalDateTime;

//  org.example.repaymentservice.model.enums.PaymentMethod;
//  org.example.repaymentservice.model.enums.PaymentStatus;

// 
// 
// 
// 
//  class PaymentResponse {
    
//      String id;
//      String paymentNumber;
//      String loanId;
//      String clientId;
//      String clientName;
//      BigDecimal amount;
//      BigDecimal penaltyAmount;
//      BigDecimal totalAmount;
//      PaymentMethod paymentMethod;
//      PaymentStatus status;
//      String receiptNumber;
//      LocalDateTime paymentDate;
//      Integer remainingInstallments;
//      BigDecimal remainingBalance;
// 