package org.example.repaymentservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.example.repaymentservice.model.Repayment;
import org.example.repaymentservice.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepaymentRepository extends JpaRepository<Repayment, String> {
    
    List<Repayment> findByLoanId(String loanId);
    
    List<Repayment> findByLoanIdAndStatus(String loanId, PaymentStatus status);
    
    Optional<Repayment> findByLoanIdAndInstallmentNumber(String loanId, Integer installmentNumber);
    
    List<Repayment> findByDueDateBeforeAndStatus(LocalDateTime date, PaymentStatus status);
    
    Page<Repayment> findByClientId(String clientId, Pageable pageable);

    //  Utiliser paidAmount au lieu de amount
    @Query("SELECT COALESCE(SUM(r.paidAmount), 0) FROM Repayment r WHERE r.paidDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    //  Utiliser paidDate au lieu de paymentDate
    @Query("SELECT COUNT(r) FROM Repayment r WHERE r.paidDate BETWEEN :startDate AND :endDate")
    Long countByDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    //  Utiliser penaltyAmount au lieu de overdueAmount
    @Query("SELECT COALESCE(SUM(r.penaltyAmount), 0) FROM Repayment r WHERE r.paidDate BETWEEN :startDate AND :endDate AND r.penaltyAmount > 0")
    BigDecimal sumOverdueAmountByDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    //  Utiliser penaltyAmount au lieu de overdueAmount
    @Query("SELECT COUNT(r) FROM Repayment r WHERE r.paidDate BETWEEN :startDate AND :endDate AND r.penaltyAmount > 0")
    Long countOverdueByDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COALESCE(SUM(r.dueAmount), 0) FROM Repayment r WHERE r.dueDate BETWEEN :startDate AND :endDate")
    BigDecimal sumDueAmountByDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    //  supplémentaires utiles
    @Query("SELECT COUNT(r) FROM Repayment r WHERE r.paidDate > r.dueDate AND r.paidDate BETWEEN :startDate AND :endDate")
    Long countLatePaymentsByDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(r) FROM Repayment r WHERE r.dueDate < :now AND r.status = 'PENDING'")
    Long countUnpaidOverdue(@Param("now") LocalDateTime now);
    
    @Query("SELECT COALESCE(SUM(r.dueAmount), 0) FROM Repayment r WHERE r.dueDate < :now AND r.status = 'PENDING'")
    BigDecimal sumUnpaidOverdueAmount(@Param("now") LocalDateTime now);

    // 
    //  POUR LISTE DE CLIENTS
    // 

    /**
    * Somme des montants payés pour une liste de clients
    */
    @Query("SELECT COALESCE(SUM(r.paidAmount), 0) FROM Repayment r " +
         "WHERE r.clientId IN :clientIds " +
         "AND r.paidDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByClientIdsAndDateBetween(@Param("clientIds") List<String> clientIds,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

   /**
   * Nombre de transactions pour une liste de clients
   */
   @Query("SELECT COUNT(r) FROM Repayment r " +
         "WHERE r.clientId IN :clientIds " +
         "AND r.paidDate BETWEEN :startDate AND :endDate")
   Long countByClientIdsAndDateBetween(@Param("clientIds") List<String> clientIds,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

   /**
   * Somme des pénalités pour une liste de clients
   */
   @Query("SELECT COALESCE(SUM(r.penaltyAmount), 0) FROM Repayment r " +
         "WHERE r.clientId IN :clientIds " +
         "AND r.paidDate BETWEEN :startDate AND :endDate " +
         "AND r.penaltyAmount > 0")
   BigDecimal sumOverdueAmountByClientIdsAndDateBetween(@Param("clientIds") List<String> clientIds,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

   /**
    * Nombre de paiements en retard pour une liste de clients
    */
   @Query("SELECT COUNT(r) FROM Repayment r " +
         "WHERE r.clientId IN :clientIds " +
         "AND r.paidDate BETWEEN :startDate AND :endDate " +
         "AND r.penaltyAmount > 0")
   Long countOverdueByClientIdsAndDateBetween(@Param("clientIds") List<String> clientIds,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

   /**
   * Somme des montants dus pour une liste de clients
   */
  @Query("SELECT COALESCE(SUM(r.dueAmount), 0) FROM Repayment r " +
         "WHERE r.clientId IN :clientIds " +
         "AND r.dueDate BETWEEN :startDate AND :endDate")
  BigDecimal sumDueAmountByClientIdsAndDateBetween(@Param("clientIds") List<String> clientIds,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

  
   /**
    * Trouve les IDs de clients distincts pour une liste de prêts
    */
  @Query("SELECT DISTINCT r.clientId FROM Repayment r WHERE r.loanId IN :loanIds")
  List<String> findDistinctClientIdsByLoanIds(@Param("loanIds") List<String> loanIds);
}
