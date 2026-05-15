package org.example.transactionservice.repository;

import org.example.transactionservice.model.StatutTransaction;
import org.example.transactionservice.model.Transaction;
import org.example.transactionservice.model.TypeTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByReference(String reference);

    Optional<Transaction> findByCampayReference(String campayReference);

    Page<Transaction> findByCompteIdOrderByDateTransactionDesc(Long compteId, Pageable pageable);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.compteId = :compteId
        AND t.dateTransaction BETWEEN :debut AND :fin
        ORDER BY t.dateTransaction DESC
    """)
    Page<Transaction> findByCompteIdAndPeriode(
            @Param("compteId") Long compteId,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin,
            Pageable pageable
    );

    Page<Transaction> findByCompteIdAndTypeTransaction(Long compteId, TypeTransaction type, Pageable pageable);

    Page<Transaction> findByStatut(StatutTransaction statut, Pageable pageable);

    List<Transaction> findByStatut(StatutTransaction statut);

    boolean existsByReference(String reference);

    long countByCompteId(Long compteId);
}
