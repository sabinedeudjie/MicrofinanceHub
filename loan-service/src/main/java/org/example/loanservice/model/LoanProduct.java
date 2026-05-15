package org.example.loanservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loan_products")
public class LoanProduct {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true, nullable = false)
    private String name;
    
    private String description;
    
    @Column(nullable = false)
    private BigDecimal minAmount;
    
    @Column(nullable = false)
    private BigDecimal maxAmount;
    
    @Column(nullable = false)
    private Integer minTermMonths;
    
    @Column(nullable = false)
    private Integer maxTermMonths;
    
    @Column(nullable = false)
    private BigDecimal interestRate;
    
    private boolean active;
}