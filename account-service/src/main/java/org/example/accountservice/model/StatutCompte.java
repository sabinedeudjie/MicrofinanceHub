package org.example.accountservice.model;

public enum StatutCompte {
    /** Créé, en attente de validation par un agent */
    EN_ATTENTE_VALIDATION,
    /** Compte opérationnel */
    ACTIF,
    /** Bloqué suite à une activité suspecte (par l'administration) */
    BLOQUE,
    /** Suspendu temporairement à la demande du client */
    SUSPENDU,
    /** Inactif suite à plus de 12 mois sans opération */
    INACTIF,
    /** Compte définitivement fermé */
    FERME,
    /** Demande rejetée par le directeur ou l'admin */
    REJETE
}
