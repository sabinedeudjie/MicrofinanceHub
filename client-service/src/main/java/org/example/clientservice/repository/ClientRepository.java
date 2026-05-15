package org.example.clientservice.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.example.clientservice.model.Client;
import org.example.clientservice.model.enums.ClientStatus;
import org.example.clientservice.model.enums.ClientType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, String>, JpaSpecificationExecutor<Client> {
    
    // 
    //  DE BASE
    // 
    
    Optional<Client> findByEmail(String email);
    
    Optional<Client> findByPhoneNumber(String phoneNumber);
    
    //  les clients actifs
    List<Client> findByStatus(ClientStatus status);
    
    List<Client> findByLastNameContainingIgnoreCase(String lastName);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsById(String id);
    
    @Query("SELECT c FROM Client c WHERE c.creditScore >= :minScore ORDER BY c.creditScore DESC")
    List<Client> findTopClientsByCreditScore(@Param("minScore") int minScore);

    // 
    //  DE RECHERCHE SIMPLES
    // 
    
    Optional<Client> findByEmailIgnoreCase(String email);
    
    Page<Client> findByStatus(ClientStatus status, Pageable pageable);
    
    List<Client> findByClientType(ClientType clientType);
    
    List<Client> findByCreditScoreGreaterThanEqual(Integer minScore);
    
    List<Client> findByCreditScoreLessThanEqual(Integer maxScore);
    
    List<Client> findByCreditScoreBetween(Integer minScore, Integer maxScore);
    
    List<Client> findByCreatedAtAfter(LocalDateTime createdAfter);
    
    // 
    //  GLOBALE
    // 
    
    @Query("SELECT c FROM Client c WHERE " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Client> searchGlobal(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT c FROM Client c WHERE " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Client> searchGlobalPaginated(@Param("searchTerm") String searchTerm, Pageable pageable);
    //  par prénom et nom
    List<Client> findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(String firstName, String lastName);

    //  par prénom
    List<Client> findByFirstNameContainingIgnoreCase(String firstName);
    Optional<Client> findByEmailContainingIgnoreCase(String email);


    // 
    // 
    // 


    //  les clients actifs (ceux avec status = ACTIVE)
    long countByStatus(ClientStatus status);
    
    //  avec @Query
    @Query("SELECT COUNT(c) FROM Client c WHERE c.status = 'ACTIVE'")
    long countActiveClients();
    
    //  les clients inactifs
    @Query("SELECT COUNT(c) FROM Client c WHERE c.status != 'ACTIVE'")
    long countInactiveClients();
     
    //  les clients créés entre deux dates
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    //  avec @Query si besoin
    @Query("SELECT COUNT(c) FROM Client c WHERE c.createdAt >= :startDate")
    long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);
    
    //  des statuts
    @Query("SELECT c.status, COUNT(c) FROM Client c GROUP BY c.status")
    List<Object[]> getClientStatusDistribution();

    /**
     * Compte le nombre total de clients
     */
    @Query("SELECT COUNT(c) FROM Client c")
    long countTotalClients();
    
    
    /**
     * Calcule la moyenne des scores de crédit
     */
    @Query("SELECT AVG(c.creditScore) FROM Client c WHERE c.creditScore IS NOT NULL")
    Double getAverageCreditScore();
    
    /**
     * Compte les clients par type
     */
    long countByClientType(ClientType clientType);
    
    
    /**
     * Recherche globale avec pagination
     */
    @Query("SELECT c FROM Client c WHERE " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Client> searchGlobal(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Trouve les clients créés par un agent spécifique
     */
    List<Client> findByCreatedBy(String createdBy);
    
    /**
     * Compte les clients créés par un agent spécifique
     */
    long countByCreatedBy(String createdBy);
    
    /**
     * Trouve les clients d'un agent avec pagination
     */
    Page<Client> findByCreatedBy(String createdBy, Pageable pageable);
    
    /**
     * Trouve les clients d'un agent par statut
     */
    List<Client> findByCreatedByAndStatus(String createdBy, ClientStatus status);
    
    /**
     * Compte les clients d'un agent par statut
     */
    @Query("SELECT COUNT(c) FROM Client c WHERE c.createdBy = :agentId AND c.status = :status")
    long countByAgentAndStatus(@Param("agentId") String agentId, @Param("status") ClientStatus status);
    
    /**
     * Trouve les clients créés après une certaine date par un agent
     */
    List<Client> findByCreatedByAndCreatedAtAfter(String createdBy, LocalDateTime date);

    /**
    * Trouve les clients créés par un agent (par email ou ID)
    */
   @Query("SELECT c FROM Client c WHERE c.createdBy = :createdBy OR c.createdBy = :createdBy")
   List<Client> findByCreatedByIgnoreCase(@Param("createdBy") String createdBy);

    //  par agence
    List<Client> findByAgencyId(String agencyId);

    List<Client> findByAgencyIdAndStatus(String agencyId, ClientStatus status);

}
