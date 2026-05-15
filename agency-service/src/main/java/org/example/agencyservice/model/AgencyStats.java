package org.example.agencyservice.model;

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
@Table(name = "agency_stats")
public class AgencyStats {
    
    @Id
    private String agencyId;
    
    @Column(nullable = false)
    @Builder.Default
    private Long totalClients = 0L;
    
    @Column(nullable = false)
    @Builder.Default
    private Long totalAccounts = 0L;
    
    @Column(nullable = false)
    @Builder.Default
    private Long totalLoans = 0L;
    
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal totalOutstanding = BigDecimal.ZERO;
    
    @Column(nullable = false)
    @Builder.Default
    private Double monthlyRepaymentRate = 0.0;
    
    @Column(nullable = false)
    @Builder.Default
    private Long activeLoans = 0L;
    
    @Column(nullable = false)
    @Builder.Default
    private Long completedLoans = 0L;
    
    @Column(nullable = false)
    @Builder.Default
    private Long defaultedLoans = 0L;
    
    private LocalDateTime updatedAt;
    
    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}