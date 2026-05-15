package org.example.repaymentservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "penalties")
public class Penalty {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String loanId;
    
    @Column(nullable = false)
    private String scheduleId;
    
    @Column(nullable = false)
    private Integer installmentNumber;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private BigDecimal dailyPenaltyRate;
    
    @Column(nullable = false)
    private Integer daysOverdue;
    
    private boolean paid;
    
    private LocalDateTime appliedDate;
    
    private LocalDateTime paidDate;
    
    private String paymentId;
    
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        appliedDate = LocalDateTime.now();
    }
}