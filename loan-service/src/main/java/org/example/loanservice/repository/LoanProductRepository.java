package org.example.loanservice.repository;

import org.example.loanservice.model.LoanProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
// java.util.Optional;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, String> {
    
    // findByCode(String code);
    
    List<LoanProduct> findByActiveTrue();
    
    Page<LoanProduct> findByActiveTrue(Pageable pageable);
    
    List<LoanProduct> findByActiveTrueAndMinAmountLessThanEqualAndMaxAmountGreaterThanEqual(
        BigDecimal amount, BigDecimal amount2);
    
    // existsByCode(String code);
}