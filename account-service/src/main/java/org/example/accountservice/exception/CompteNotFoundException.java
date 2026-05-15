package org.example.accountservice.exception;

/**
 * Exception levée quand un compte n'est pas trouvé.
 * Provoque une réponse HTTP 404.
 */
public class CompteNotFoundException extends RuntimeException {
    public CompteNotFoundException(Long id) {
        super("Compte introuvable avec l'id : " + id);
    }
    public CompteNotFoundException(String numeroCompte) {
        super("Compte introuvable avec le numéro : " + numeroCompte);
    }
}
