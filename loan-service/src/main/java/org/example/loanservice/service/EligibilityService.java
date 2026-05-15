package org.example.loanservice.service;

import org.example.loanservice.client.AccountServiceClient;
import org.example.loanservice.client.ClientServiceClient;
import org.example.loanservice.client.model.AccountInfo;
import org.example.loanservice.client.model.ClientInfo;
import org.example.loanservice.dto.response.EligibilityResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EligibilityService {
    
    private final ClientServiceClient clientServiceClient;
    private final AccountServiceClient accountServiceClient;
    
    private static final BigDecimal MAX_LOAN_AMOUNT = new BigDecimal("5000000");
    private static final BigDecimal MIN_LOAN_AMOUNT = new BigDecimal("50000");
    private static final int MAX_TERM_MONTHS = 36;
    private static final int MIN_TERM_MONTHS = 1;
    private static final int MIN_CREDIT_SCORE = 60;
    
    /**
     * Vérifie l'éligibilité d'un client avec un compte spécifique
     */
    public EligibilityResponse checkEligibility(String clientId, String accountNumber, 
                                                 BigDecimal requestedAmount, Integer termMonths, 
                                                 String token) {
        log.info("d'éligibilité pour le client: {}, compte: {}", clientId, accountNumber);
        
        // 
        //  1: Vérifier que le client existe et récupérer ses infos
        // 
        ClientInfo clientInfo = null;
        try {
            clientInfo = clientServiceClient.getClientInfo(clientId, token);
            log.info("trouvé: {} {}, email: {}", 
                clientInfo.getFirstName(), 
                clientInfo.getLastName(), 
                clientInfo.getEmail());
        } catch (FeignException.NotFound e) {
            return EligibilityResponse.builder()
                .eligible(false)
                .message("Client non trouvé. Veuillez vous inscrire d'abord.")
                .clientId(clientId)
                .build();
        } catch (FeignException e) {
            log.error("Client Service: {}", e.getMessage());
            return EligibilityResponse.builder()
                .eligible(false)
                .message("Service client indisponible. Code erreur: " + e.status())
                .clientId(clientId)
                .build();
        }
        
        // 
        //  2: Vérifier que le compte existe et appartient au client
        // 
        AccountInfo account = null;
        try {
            account = accountServiceClient.getAccountByNumber(accountNumber, token);
            
            if (account == null) {
                return EligibilityResponse.builder()
                    .eligible(false)
                    .message("Le compte " + accountNumber + " n'existe pas. Veuillez vérifier le numéro.")
                    .clientId(clientId)
                    .build();
            }
            
            //  que le compte appartient au client
            if (!account.getClientId().equals(clientId)) {
                return EligibilityResponse.builder()
                    .eligible(false)
                    .message("Le compte " + accountNumber + " n'appartient pas au client " + clientId)
                    .clientId(clientId)
                    .build();
            }
            
            //  que le compte est actif
            if (!"ACTIVE".equals(account.getStatus())) {
                return EligibilityResponse.builder()
                    .eligible(false)
                    .message("Le compte " + accountNumber + " n'est pas actif. Statut actuel: " + account.getStatus())
                    .clientId(clientId)
                    .build();
            }
            
            log.info("validé: {} - Solde: {}", accountNumber, account.getBalance());
            
        } catch (FeignException.NotFound e) {
            return EligibilityResponse.builder()
                .eligible(false)
                .message("Compte " + accountNumber + " non trouvé. Veuillez vérifier le numéro de compte.")
                .clientId(clientId)
                .build();
        } catch (FeignException.ServiceUnavailable e) {
            log.error("Account indisponible (503): {}", e.getMessage());
            return EligibilityResponse.builder()
                .eligible(false)
                .message("Le service des comptes est temporairement indisponible. Veuillez réessayer dans quelques instants.")
                .clientId(clientId)
                .build();
        } catch (FeignException e) {
            int statusCode = e.status();
            log.error("Account Service - Code: {}, Message: {}", statusCode, e.getMessage());
            
            String errorMessage;
            if (statusCode == 404) {
                errorMessage = "Compte " + accountNumber + " non trouvé.";
            } else if (statusCode == 403) {
                errorMessage = "Accès non autorisé au service des comptes.";
            } else if (statusCode == 500) {
                errorMessage = "Erreur interne du service des comptes.";
            } else {
                errorMessage = "Service des comptes indisponible (Code: " + statusCode + "). Veuillez réessayer.";
            }
            
            return EligibilityResponse.builder()
                .eligible(false)
                .message(errorMessage)
                .clientId(clientId)
                .build();
        }
        
        // 
        //  3: Vérifier les montants
        // 
        if (requestedAmount.compareTo(MIN_LOAN_AMOUNT) < 0) {
            return EligibilityResponse.builder()
                .eligible(false)
                .message("Le montant minimum est de " + MIN_LOAN_AMOUNT + " FCFA")
                .build();
        }
        
        if (requestedAmount.compareTo(MAX_LOAN_AMOUNT) > 0) {
            return EligibilityResponse.builder()
                .eligible(false)
                .message("Le montant maximum est de " + MAX_LOAN_AMOUNT + " FCFA")
                .build();
        }
        
        // 
        //  4: Vérifier la durée
        // 
        if (termMonths < MIN_TERM_MONTHS || termMonths > MAX_TERM_MONTHS) {
            return EligibilityResponse.builder()
                .eligible(false)
                .message("La durée doit être entre " + MIN_TERM_MONTHS + " et " + MAX_TERM_MONTHS + " mois")
                .build();
        }
        
        // 
        //  5: Vérifier le score de crédit
        // 
        Integer creditScore = null;
        try {
            creditScore = clientServiceClient.getClientCreditScore(clientId, token);
            log.info("de crédit récupéré: {}", creditScore);
        } catch (FeignException.NotFound e) {
            return EligibilityResponse.builder()
                .eligible(false)
                .message("Client non trouvé dans le service de scoring.")
                .clientId(clientId)
                .build();
        } catch (FeignException.ServiceUnavailable e) {
            return EligibilityResponse.builder()
                .eligible(false)
                .message("Service de scoring indisponible. Veuillez réessayer.")
                .clientId(clientId)
                .build();
        } catch (FeignException e) {
            log.warn("lors de la récupération du score: {}", e.getMessage());
        }
        
        if (creditScore == null || creditScore < MIN_CREDIT_SCORE) {
            return EligibilityResponse.builder()
                .eligible(false)
                .message("Score de crédit insuffisant. Minimum requis: " + MIN_CREDIT_SCORE + 
                        ". Votre score: " + (creditScore != null ? creditScore : "Non disponible"))
                .clientId(clientId)
                .build();
        }
        
        // 
        //  6: Vérifier les prêts actifs existants
        // 
        //  vérification pourrait être ajoutée via un appel au Loan Service lui-même
        //  la récursion en appelant directement le repository si nécessaire
        
        // 
        //  7: Client éligible
        // 
        BigDecimal maxEligibleAmount = calculateMaxEligibleAmount(creditScore);
        
        return EligibilityResponse.builder()
            .eligible(true)
            .maxEligibleAmount(maxEligibleAmount)
            .maxTermMonths(MAX_TERM_MONTHS)
            .message("Client éligible")
            .clientId(clientId)
            .clientEmail(clientInfo.getEmail())
            .clientName(clientInfo.getFirstName() + " " + clientInfo.getLastName())
            .accountNumber(accountNumber)
            .accountBalance(account.getBalance())
            .build();
    }
    
    /**
     * Calcule le montant maximum éligible en fonction du score de crédit
     */
    private BigDecimal calculateMaxEligibleAmount(Integer creditScore) {
        if (creditScore == null) return BigDecimal.ZERO;
        if (creditScore >= 80) {
            return MAX_LOAN_AMOUNT;
        } else if (creditScore >= 70) {
            return new BigDecimal("3000000");
        } else if (creditScore >= 60) {
            return new BigDecimal("1000000");
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Vérifie l'éligibilité sans numéro de compte spécifique
     * Récupère automatiquement le premier compte actif du client
     */
    public EligibilityResponse checkEligibility(String clientId, BigDecimal requestedAmount, 
                                                 Integer termMonths, String token) {
        log.info("d'éligibilité pour le client: {}, montant: {}, durée: {}", 
            clientId, requestedAmount, termMonths);

        List<AccountInfo> accounts = accountServiceClient.getAccountsByClientId(clientId, token);

        if (accounts == null || accounts.isEmpty()) {
            return EligibilityResponse.builder()
                .eligible(false)
                .message("Aucun compte trouvé pour ce client. Veuillez d'abord ouvrir un compte.")
                .clientId(clientId)
                .build();
        }

        // . Trouver le premier compte actif
        String activeAccountNumber = accounts.stream()
            .filter(a -> "ACTIVE".equals(a.getStatus()))
            .map(AccountInfo::getAccountNumber)
            .findFirst()
            .orElse(null);

        if (activeAccountNumber == null) {
            return EligibilityResponse.builder()
                .eligible(false)
                .message("Aucun compte actif trouvé pour ce client.")
                .clientId(clientId)
                .build();
        }

        log.info("actif trouvé: {}", activeAccountNumber);

        // . Appeler la méthode complète avec le numéro de compte
        return checkEligibility(clientId, activeAccountNumber, requestedAmount, termMonths, token);
    }
    
    /**
     * Vérifie si un client est éligible pour un prêt (version simplifiée)
     * @return true si éligible, false sinon
     */
    public boolean isEligible(String clientId, String token) {
        try {
            //  les informations du client
            ClientInfo clientInfo = clientServiceClient.getClientInfo(clientId, token);
            if (clientInfo == null) {
                log.warn("non trouvé: {}", clientId);
                return false;
            }
            
            //  le score de crédit
            Integer creditScore = clientServiceClient.getClientCreditScore(clientId, token);
            if (creditScore == null || creditScore < MIN_CREDIT_SCORE) {
                log.warn("de crédit insuffisant pour client {}: {}", clientId, creditScore);
                return false;
            }
            
            //  l'existence d'un compte actif
            List<AccountInfo> accounts = accountServiceClient.getAccountsByClientId(clientId, token);
            boolean hasActiveAccount = accounts != null && accounts.stream()
                .anyMatch(a -> "ACTIVE".equals(a.getStatus()));
            
            if (!hasActiveAccount) {
                log.warn("compte actif pour client: {}", clientId);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("lors de la vérification d'éligibilité pour {}: {}", clientId, e.getMessage());
            return false;
        }
    }
    private String extractErrorMessage(FeignException e) {
    try {
        if (e.contentUTF8() != null && !e.contentUTF8().isEmpty()) {
            //  de parser le JSON d'erreur
            com.fasterxml.jackson.databind.ObjectMapper mapper = 
                new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = 
                mapper.readTree(e.contentUTF8());
            if (node.has("message")) {
                return node.get("message").asText();
            }
            if (node.has("error")) {
                return node.get("error").asText();
            }
        }
    } catch (Exception ex) {
        log.debug("de parser l'erreur: {}", ex.getMessage());
    }
    
    //  par défaut basé sur le code HTTP
    switch (e.status()) {
        case 404:
            return "Ressource non trouvée";
        case 403:
            return "Accès non autorisé";
        case 500:
            return "Erreur interne du service";
        default:
            return "Erreur de communication: " + e.getMessage();
     }
   }
}
