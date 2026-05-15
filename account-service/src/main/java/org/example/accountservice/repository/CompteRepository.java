package org.example.accountservice.repository;

import org.example.accountservice.model.Compte;
import org.example.accountservice.model.StatutCompte;
import org.example.accountservice.model.TypeCompte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompteRepository extends JpaRepository<Compte, Long>, JpaSpecificationExecutor<Compte> {

    /** Trouver un compte par son numéro */
    Optional<Compte> findByNumeroCompte(String numeroCompte);

    /** Trouver tous les comptes d'un client (paginés) */
    Page<Compte> findByClientId(String clientId, Pageable pageable);

    /** Trouver les comptes d'un client par statut (paginés) */
    Page<Compte> findByClientIdAndStatut(String clientId, StatutCompte statut, Pageable pageable);

    /** Vérifier si un numéro de compte existe déjà */
    boolean existsByNumeroCompte(String numeroCompte);

    /** Trouver les comptes par type (paginés) */
    Page<Compte> findByTypeCompte(TypeCompte typeCompte, Pageable pageable);

    /** Compter les comptes actifs d'un client */
    long countByClientIdAndStatut(String clientId, StatutCompte statut);

    /** Trouver tous les comptes ayant un statut donné (paginés) */
    Page<Compte> findByStatut(StatutCompte statut, Pageable pageable);

    /** Trouver les comptes dont le solde est inférieur au minimum (paginés) */
    @Query("SELECT c FROM Compte c WHERE c.solde < c.soldeMinimum AND c.statut = org.example.accountservice.model.StatutCompte.ACTIF")
    Page<Compte> findComptesAvecSoldeSousMinimum(Pageable pageable);

    /** Calcul du total des soldes actifs par client */
    @Query("SELECT SUM(c.solde) FROM Compte c WHERE c.clientId = :clientId AND c.statut = org.example.accountservice.model.StatutCompte.ACTIF")
    BigDecimal sumSoldeByClientId(@Param("clientId") String clientId);

    /** Trouver les transactions en attente pour la relance (sans pagination — usage interne) */
    List<Compte> findByStatut(StatutCompte statut);

    /** Vérifie si un client a déjà un compte d'un type donné dans un statut non terminal */
    boolean existsByClientIdAndTypeCompteAndStatutIn(String clientId, TypeCompte typeCompte, List<StatutCompte> statuts);

    /** Compte le nombre de clients distincts ayant au moins un compte */
    @Query("SELECT COUNT(DISTINCT c.clientId) FROM Compte c")
    long countDistinctClients();

    /** Somme des soldes de tous les comptes actifs */
    @Query("SELECT COALESCE(SUM(c.solde), 0) FROM Compte c WHERE c.statut = org.example.accountservice.model.StatutCompte.ACTIF")
    java.math.BigDecimal sumAllSoldesActifs();
}
