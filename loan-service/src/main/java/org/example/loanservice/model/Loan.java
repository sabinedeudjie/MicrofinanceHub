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
import java.util.ArrayList;
import java.util.List;

import org.example.loanservice.model.enums.LoanStatus;
import org.example.loanservice.model.enums.RepaymentFrequency;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loans")
public class Loan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String loanNumber;

    @Column(name = "application_id")
    private String applicationId;
    
    @Column(nullable = false)
    private String clientId;
    
    @Column(nullable = false)
    private String clientEmail;
    
    @Column(nullable = false)
    private String clientFirstName;
    
    @Column(nullable = false)
    private String clientLastName;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private BigDecimal interestRate;
    
    @Column(nullable = false)
    private Integer termMonths;
    
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private RepaymentFrequency repaymentFrequency;
    
    @Column(nullable = false)
    private BigDecimal monthlyPayment;
    
    @Column(nullable = false)
    private BigDecimal totalRepayment;
    
    @Column(nullable = false)
    private BigDecimal remainingBalance;
    
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private LoanStatus status;
    
    private String purpose;
    
    private LocalDateTime applicationDate;
    
    private LocalDateTime approvalDate;
    
    private LocalDateTime disbursementDate;
    
    private LocalDateTime nextPaymentDate;
    
    private LocalDateTime maturityDate;
    
    // String approvedBy;
    
    private String rejectionReason;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    @Column(name = "approved_by")
    private String approvedBy;  // 'agent qui a approuvé le prêt

    @Column(name = "disbursed_by")
    private String disbursedBy;  // 'agent qui a effectué le décaissement

    @Column(name = "reviewed_by")
    private String reviewedBy;  // 'agent qui a examiné la demande

    @Column(name = "loan_product_id")
    private String loanProductId;

    @Column(name = "loan_product_name")
    private String loanProductName;
    
    @Builder.Default
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AmortizationSchedule> amortizationSchedules = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        loanNumber = generateLoanNumber();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    private String generateLoanNumber() {
        return "LN" + System.currentTimeMillis();
    }
}