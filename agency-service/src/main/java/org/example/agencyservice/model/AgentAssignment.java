package org.example.agencyservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "agent_assignments")
public class AgentAssignment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "agent_id", nullable = false)
    private String agentId;
    
    @Column(name = "agent_email", nullable = false)
    private String agentEmail;
    
    @Column(name = "agent_name", nullable = false)
    private String agentName;
    
    @Column(name = "agency_id", nullable = false)
    private String agencyId;
    
    @Column(name = "agency_code")
    private String agencyCode;
    
    @Column(name = "role")
    @Builder.Default
    private String role = "AGENT";
    
    @Column(name = "assigned_by")
    private String assignedBy;
    
    @Column(name = "assigned_by_name")
    private String assignedByName;
    
    @Column(name = "reason", columnDefinition = "TEXT")  // ←  que ce champ existe
    private String reason;
    
    @Column(name = "reference", unique = true)  // ←  que ce champ existe
    private String reference;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    @Column(name = "active")
    @Builder.Default
    private boolean active = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}