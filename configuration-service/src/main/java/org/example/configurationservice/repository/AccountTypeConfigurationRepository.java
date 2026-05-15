package org.example.configurationservice.repository;

import org.example.configurationservice.model.AccountTypeConfiguration;
import org.example.configurationservice.model.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountTypeConfigurationRepository extends JpaRepository<AccountTypeConfiguration, String> {
    
    Optional<AccountTypeConfiguration> findByCode(String code);
    
    List<AccountTypeConfiguration> findByCategoryIdAndActiveTrue(String categoryId);
    
    
    List<AccountTypeConfiguration> findByAccountTypeAndActiveTrue(AccountType accountType);
    
    Optional<AccountTypeConfiguration> findByCategoryIdAndIsDefaultTrue(String categoryId);
    
    boolean existsByCode(String code);
    
    boolean existsByNameAndAccountType(String name, AccountType accountType);
    
    Optional<AccountTypeConfiguration> findByAccountType(org.example.configurationservice.model.enums.AccountType accountType);
    
    List<AccountTypeConfiguration> findByActiveTrue();
    
    Optional<AccountTypeConfiguration> findByNameIgnoreCase(String name);
}