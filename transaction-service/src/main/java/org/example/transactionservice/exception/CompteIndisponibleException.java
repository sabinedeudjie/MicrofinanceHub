package org.example.transactionservice.exception;

public class CompteIndisponibleException extends RuntimeException {
    public CompteIndisponibleException(Long compteId) {
        super("Compte introuvable ou indisponible : " + compteId);
    }

    public CompteIndisponibleException(String numero) {
        super("Compte introuvable : " + numero);
    }
}
