package org.example.repaymentservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.example.repaymentservice.model.enums.PaymentMethod;
import org.example.repaymentservice.model.enums.PaymentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true, nullable = false)
    private String paymentNumber;
    
    @Column(nullable = false)
    private String loanId;
    
    @Column(nullable = false)
    private String clientId;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    private BigDecimal penaltyAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    
    private String transactionId;
    
    private String reference;
    
    private String receiptNumber;
    
    private LocalDateTime paymentDate;
    
    private String paidBy;

    private String numeroPaiement;
    
    private String notes;
    
    private Long compteSourceId;
    
    private String validatedBy;
    
    private LocalDateTime validatedAt;
    
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        paymentDate = LocalDateTime.now();
        paymentNumber = "PAY" + System.currentTimeMillis();
    }
}