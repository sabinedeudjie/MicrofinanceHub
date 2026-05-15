package org.example.configurationservice.repository;

import org.example.configurationservice.model.AccountCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountCategoryRepository extends JpaRepository<AccountCategory, String> {
    
    Optional<AccountCategory> findByCode(String code);
    
    List<AccountCategory> findByActiveTrueOrderByDisplayOrderAsc();
    
    boolean existsByCode(String code);
}