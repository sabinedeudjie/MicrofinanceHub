package org.example.reportingservice.model;

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
@Table(name = "kpis")
public class Kpi {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Column(nullable = false)
    private String category;
    
    @Column(nullable = false)
    private BigDecimal value;
    
    private String unit;
    
    private LocalDateTime periodStart;
    
    private LocalDateTime periodEnd;
    
    private LocalDateTime calculatedAt;
    
    private String calculatedBy;
    
    @Column(columnDefinition = "jsonb")
    private String metadata;
}