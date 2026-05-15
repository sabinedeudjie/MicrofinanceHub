package org.example.configurationservice.repository;

import org.example.configurationservice.model.MicrofinanceConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MicrofinanceConfigurationRepository extends JpaRepository<MicrofinanceConfiguration, String> {
    
    Optional<MicrofinanceConfiguration> findByActiveTrue();
    
    Optional<MicrofinanceConfiguration> findByMicrofinanceCode(String microfinanceCode);
    
    boolean existsByMicrofinanceCode(String microfinanceCode);
}