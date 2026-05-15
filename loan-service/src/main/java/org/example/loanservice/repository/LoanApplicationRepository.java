package org.example.loanservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import org.example.loanservice.model.LoanApplication;
import org.example.loanservice.model.enums.ApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, String> {
    
    Optional<LoanApplication> findByApplicationNumber(String applicationNumber);
    
    List<LoanApplication> findByClientId(String clientId);
    
    List<LoanApplication> findByStatus(ApplicationStatus status);
    
    List<LoanApplication> findByClientIdAndStatus(String clientId, ApplicationStatus status);
    
    boolean existsByClientIdAndStatus(String clientId, ApplicationStatus status);

    Page<LoanApplication> findByStatus(ApplicationStatus status, Pageable pageable);

    List<LoanApplication> findByApplicationDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM LoanApplication a WHERE a.applicationDate BETWEEN :startDate AND :endDate AND a.status = :status")
    Long countByStatusAndApplicationDateBetween(@Param("status") ApplicationStatus status,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);
                    
    @Query("SELECT COUNT(a) FROM LoanApplication a WHERE a.applicationDate BETWEEN :startDate AND :endDate")
    Long countByApplicationDateBetween(@Param("startDate") LocalDateTime startDate, 
                                        @Param("endDate") LocalDateTime endDate);
    

    @Query("SELECT COALESCE(SUM(a.requestedAmount), 0) FROM LoanApplication a WHERE a.applicationDate BETWEEN :startDate AND :endDate")
    BigDecimal sumRequestedAmountByDateBetween(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    // 
    //  POUR PLUSIEURS CLIENTS
    // 

    @Query("SELECT l FROM LoanApplication l WHERE l.clientId IN :clientIds AND l.applicationDate BETWEEN :startDate AND :endDate")
    List<LoanApplication> findByClientIdInAndApplicationDateBetween(
            @Param("clientIds") List<String> clientIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    List<LoanApplication> findByClientIdIn(List<String> clientIds);

    Page<LoanApplication> findByClientIdInAndStatus(List<String> clientIds, ApplicationStatus status, Pageable pageable);

    @Query("SELECT COUNT(l) FROM LoanApplication l WHERE l.clientId IN :clientIds AND l.status = :status")
    Long countByClientIdInAndStatus(
            @Param("clientIds") List<String> clientIds,
            @Param("status") ApplicationStatus status);
    
    @Query("SELECT COUNT(l) FROM LoanApplication l WHERE l.clientId IN :clientIds AND l.status = :status AND l.applicationDate BETWEEN :startDate AND :endDate")
    Long countByClientIdInAndStatusAndApplicationDateBetween(
            @Param("clientIds") List<String> clientIds,
            @Param("status") ApplicationStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 
    //  POUR AGENT (FILTRAGE PAR REVIEWEDBY)
    // 
    
    /**
     * Trouve les IDs de clients distincts pour lesquels l'agent a examiné des demandes
     * @param reviewedBy L'ID ou l'email de l'agent
     * @return Liste des IDs de clients distincts
     */
    @Query("SELECT DISTINCT l.clientId FROM LoanApplication l WHERE l.reviewedBy = :reviewedBy")
    List<String> findDistinctClientIdsByReviewedBy(@Param("reviewedBy") String reviewedBy);
    
    /**
     * Trouve les IDs de clients distincts pour lesquels l'agent a examiné des demandes
     * avec filtre par date
     */
    @Query("SELECT DISTINCT l.clientId FROM LoanApplication l WHERE l.reviewedBy = :reviewedBy AND l.reviewedDate BETWEEN :startDate AND :endDate")
    List<String> findDistinctClientIdsByReviewedByAndDateBetween(
            @Param("reviewedBy") String reviewedBy,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    /**
     * Compte le nombre de clients distincts pour un agent
     */
    @Query("SELECT COUNT(DISTINCT l.clientId) FROM LoanApplication l WHERE l.reviewedBy = :reviewedBy")
    Long countDistinctClientIdsByReviewedBy(@Param("reviewedBy") String reviewedBy);
    
    /**
     * Trouve toutes les demandes examinées par un agent
     */
    List<LoanApplication> findByReviewedBy(String reviewedBy);
    
    /**
     * Trouve toutes les demandes examinées par un agent sur une période
     */
    List<LoanApplication> findByReviewedByAndReviewedDateBetween(String reviewedBy, 
                                                                  LocalDateTime startDate, 
                                                                  LocalDateTime endDate);
    
    /**
     * Compte les demandes par statut pour un agent
     */
    @Query("SELECT COUNT(l) FROM LoanApplication l WHERE l.reviewedBy = :reviewedBy AND l.status = :status")
    Long countByReviewedByAndStatus(@Param("reviewedBy") String reviewedBy, 
                                     @Param("status") ApplicationStatus status);
}