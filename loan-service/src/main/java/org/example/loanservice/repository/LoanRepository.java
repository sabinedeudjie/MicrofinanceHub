package org.example.loanservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.example.loanservice.model.Loan;
import org.example.loanservice.model.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, String> {
    
    Optional<Loan> findByLoanNumber(String loanNumber);
    
    List<Loan> findByClientId(String clientId);
    
    List<Loan> findByClientIdAndStatus(String clientId, LoanStatus status);
    
    Page<Loan> findByStatus(LoanStatus status, Pageable pageable);
    
    List<Loan> findByStatusAndNextPaymentDateBefore(LoanStatus status, LocalDateTime date);
    
    Long countByClientIdAndStatus(String clientId, LoanStatus status);

    Long countByStatus(LoanStatus status);
    
    List<Loan> findByDisbursementDateBetween(LocalDateTime start, LocalDateTime end);

    List<Loan> findByStatus(LoanStatus status);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.disbursementDate BETWEEN :startDate AND :endDate AND l.status IN :statuses")
Long countByDisbursementDateBetweenAndStatusIn(@Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate,
                                                @Param("statuses") List<LoanStatus> statuses);

@Query("SELECT COUNT(l) FROM Loan l WHERE l.disbursementDate BETWEEN :startDate AND :endDate AND l.status = :status")
Long countByDisbursementDateBetweenAndStatus(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate,
                                              @Param("status") LoanStatus status);

@Query("SELECT COALESCE(SUM(l.amount), 0) FROM Loan l WHERE l.disbursementDate BETWEEN :startDate AND :endDate")
BigDecimal sumAmountByDisbursementDateBetween(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

@Query("SELECT COALESCE(SUM(l.totalRepayment - l.remainingBalance), 0) FROM Loan l WHERE l.disbursementDate BETWEEN :startDate AND :endDate")
BigDecimal sumRepaidAmountByDisbursementDateBetween(@Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

@Query("SELECT COALESCE(SUM(l.remainingBalance), 0) FROM Loan l WHERE l.status = :status")
BigDecimal sumRemainingBalanceByStatus(@Param("status") LoanStatus status);

@Query("SELECT COALESCE(SUM(l.amount), 0) FROM Loan l WHERE l.status = :status")
BigDecimal sumAmountByStatus(@Param("status") LoanStatus status);

@Query("SELECT COALESCE(SUM(l.totalRepayment - l.remainingBalance), 0) FROM Loan l WHERE l.status = :status")
BigDecimal sumRepaidAmountByStatus(@Param("status") LoanStatus status);

@Query("SELECT COALESCE(SUM(l.monthlyPayment), 0) FROM Loan l WHERE l.nextPaymentDate IS NOT NULL AND l.nextPaymentDate < :date AND l.status = 'ACTIVE'")
BigDecimal sumMonthlyPaymentByNextPaymentDateBefore(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.clientId IN :clientIds AND l.status = :status")
    Long countByClientIdInAndStatus(
            @Param("clientIds") List<String> clientIds,
            @Param("status") LoanStatus status);


  List<Loan> findByClientIdIn(List<String> clientIds);

  Page<Loan> findByClientIdIn(List<String> clientIds, Pageable pageable);

  Page<Loan> findByClientIdInAndStatus(List<String> clientIds, LoanStatus status, Pageable pageable);

/**
 // Trouve les prêts par liste d'IDs clients et statut
 */
@Query("SELECT l FROM Loan l WHERE l.clientId IN :clientIds AND l.status = :status")
List<Loan> findByClientIdInAndStatus(@Param("clientIds") List<String> clientIds, 
                                      @Param("status") LoanStatus status);

/**
 * Trouve les prêts par liste d'IDs clients et date de décaissement
 */
@Query("SELECT l FROM Loan l WHERE l.clientId IN :clientIds AND l.disbursementDate BETWEEN :startDate AND :endDate")
List<Loan> findByClientIdInAndDisbursementDateBetween(@Param("clientIds") List<String> clientIds,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

/**
 * Compte les prêts par liste d'IDs clients, statut et date de décaissement
 */
@Query("SELECT COUNT(l) FROM Loan l WHERE l.clientId IN :clientIds AND l.status = :status AND l.disbursementDate BETWEEN :startDate AND :endDate")
Long countByClientIdInAndStatusAndDisbursementDateBetween(@Param("clientIds") List<String> clientIds,
                                                           @Param("status") LoanStatus status,
                                                           @Param("startDate") LocalDateTime startDate,
                                                           @Param("endDate") LocalDateTime endDate);

/**
 * Somme des montants décaissés par liste d'IDs clients
 */
@Query("SELECT COALESCE(SUM(l.amount), 0) FROM Loan l WHERE l.clientId IN :clientIds")
BigDecimal sumAmountByClientIdIn(@Param("clientIds") List<String> clientIds);

/**
 * Somme des montants remboursés par liste d'IDs clients
 */
@Query("SELECT COALESCE(SUM(l.totalRepayment - l.remainingBalance), 0) FROM Loan l WHERE l.clientId IN :clientIds")
BigDecimal sumRepaidAmountByClientIdIn(@Param("clientIds") List<String> clientIds);

/**
 * Somme des soldes restants par liste d'IDs clients et statut
 */
@Query("SELECT COALESCE(SUM(l.remainingBalance), 0) FROM Loan l WHERE l.clientId IN :clientIds AND l.status = :status")
BigDecimal sumRemainingBalanceByClientIdInAndStatus(@Param("clientIds") List<String> clientIds,
                                                     @Param("status") LoanStatus status);
                                                
                                                
@Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Loan l WHERE l.applicationId = :applicationId")
boolean existsByApplicationId(@Param("applicationId") String applicationId);

}