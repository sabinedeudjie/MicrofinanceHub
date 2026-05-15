package org.example.repaymentservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "schedules")
public class Schedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id; 
    
    @Column(name = "loan_id", nullable = false)
    private String loanId;
    
    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;
    
    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;
    
    @Column(name = "due_amount", nullable = false)
    private BigDecimal dueAmount;
    
    @Column(name = "principal_amount", nullable = false)
    private BigDecimal principalAmount;
    
    @Column(name = "interest_amount", nullable = false)
    private BigDecimal interestAmount;
    
    @Column(name = "remaining_balance", nullable = false)
    private BigDecimal remainingBalance;
    
    private boolean paid;
    
    @Column(name = "paid_date")
    private LocalDateTime paidDate;
    
    @Column(name = "payment_id")
    private String paymentId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public BigDecimal getPaidAmount() {
        return paid ? dueAmount : BigDecimal.ZERO;
    }
    
    public BigDecimal getRemainingAmount() {
        return paid ? BigDecimal.ZERO : dueAmount;
    }
    
    public BigDecimal getTotalAmount() {
        return dueAmount;
    }
    
    public String getStatus() {
        return paid ? "PAID" : "PENDING";
    }
}