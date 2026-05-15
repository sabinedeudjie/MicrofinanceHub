package org.example.loanservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.example.loanservice.model.enums.ApplicationStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loan_applications")
public class LoanApplication {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String applicationNumber;
    
    @Column(nullable = false)
    private String clientId;

    @Column(name = "account_number")
    private String accountNumber;
    
    @Column(nullable = false)
    private String clientEmail;
    
    @Column(nullable = false)
    private String clientFirstName;
    
    @Column(nullable = false)
    private String clientLastName;
    
    @Column(nullable = false)
    private BigDecimal requestedAmount;
    
    @Column(nullable = false)
    private Integer termMonths;
    
    private String purpose;
    
    private BigDecimal monthlyIncome;
    
    private String employmentStatus;
    
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private ApplicationStatus status;
    
    private String rejectionReason;
    
    private LocalDateTime applicationDate;
    
    private LocalDateTime reviewedDate;
    
    private String reviewedBy;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        applicationDate = LocalDateTime.now();
        status = ApplicationStatus.PENDING;
        applicationNumber = "APP" + System.currentTimeMillis();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}