package org.example.agencyservice.repository;

import org.example.agencyservice.model.AgencyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface AgencyStatsRepository extends JpaRepository<AgencyStats, String> {
    
    Optional<AgencyStats> findByAgencyId(String agencyId);
    
    @Modifying
    @Transactional
    @Query("UPDATE AgencyStats s SET s.totalClients = s.totalClients + :increment WHERE s.agencyId = :agencyId")
    void incrementTotalClients(@Param("agencyId") String agencyId, @Param("increment") Long increment);
    
    @Modifying
    @Transactional
    @Query("UPDATE AgencyStats s SET s.totalAccounts = s.totalAccounts + :increment WHERE s.agencyId = :agencyId")
    void incrementTotalAccounts(@Param("agencyId") String agencyId, @Param("increment") Long increment);
}