package org.example.accountservice.model;

public enum TypeCompte {
    /** Compte épargne classique avec taux d'intérêt */
    EPARGNE,
    /** Compte courant pour les opérations quotidiennes */
    COURANT,
    /** Dépôt à terme bloqué pendant une durée fixe */
    DEPOT_A_TERME,
    /** Compte micro-épargne pour les petits épargnants */
    MICRO_EPARGNE,
    /** Compte lié à un produit de crédit */
    CREDIT
}
