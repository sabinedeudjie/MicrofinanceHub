package org.example.agencyservice.repository;

import org.example.agencyservice.model.Agency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgencyRepository extends JpaRepository<Agency, String> {
    Optional<Agency> findByCode(String code);
    Optional<Agency> findByDirectorId(String directorId);
    List<Agency> findByStatus(String status);
    List<Agency> findByRegion(String region);
    boolean existsByCode(String code);
    boolean existsByDirectorId(String directorId);
    Optional<Agency> findByDirectorEmail(String directorEmail);
}