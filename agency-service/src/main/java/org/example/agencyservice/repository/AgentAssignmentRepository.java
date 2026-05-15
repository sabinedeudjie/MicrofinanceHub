package org.example.agencyservice.repository;

import org.example.agencyservice.model.AgentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgentAssignmentRepository extends JpaRepository<AgentAssignment, String> {
    
    Optional<AgentAssignment> findByAgentIdAndActiveTrue(String agentId);

    List<AgentAssignment> findAllByActiveTrue();
    
    List<AgentAssignment> findByAgencyIdAndActiveTrue(String agencyId);
    
    List<AgentAssignment> findByAgencyId(String agencyId);
    
    boolean existsByAgentIdAndActiveTrue(String agentId);
    
    @Modifying
    @Query("UPDATE AgentAssignment a SET a.active = false WHERE a.agentId = :agentId AND a.active = true")
    void deactivateAgentAssignments(@Param("agentId") String agentId);
    
    @Modifying
    @Query("UPDATE AgentAssignment a SET a.active = false WHERE a.agencyId = :agencyId")
    void deactivateAllAssignmentsByAgency(@Param("agencyId") String agencyId);
    
    long countByAgencyIdAndActiveTrue(String agencyId);

    Optional<AgentAssignment> findByAgentEmailAndActiveTrue(String agentEmail);

    List<AgentAssignment> findByAgentIdAndAgencyIdOrderByAssignedAtDesc(String agentId, String agencyId);
}