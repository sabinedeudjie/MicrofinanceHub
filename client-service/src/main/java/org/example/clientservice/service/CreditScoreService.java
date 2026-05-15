package org.example.clientservice.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.example.clientservice.dto.response.CreditScoreResponse;
import org.example.clientservice.model.Client;
import org.example.clientservice.repository.ClientRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditScoreService {
    
    private final ClientRepository clientRepository;
    
    @Transactional
    public void updateCreditScore(String clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        
        //  du score de crédit basé sur plusieurs facteurs
        int score = calculateCreditScore(client);
        
        client.setCreditScore(score);
        clientRepository.save(client);
        
        log.info("de crédit mis à jour pour {}: {}", client.getEmail(), score);
    }
    
    private int calculateCreditScore(Client client) {
        int score = 50; //  de base
        
        //  1: Âge du client
        if (client.getBirthDate() != null) {
            int age = java.time.LocalDateTime.now().getYear() - client.getBirthDate().getYear();
            if (age >= 25 && age <= 60) {
                score += 10;
            } else if (age > 60) {
                score += 5;
            }
        }
        
        //  2: Type de client
        switch (client.getClientType()) {
            case BUSINESS:
                score += 15;
                break;
            case GROUP:
                score += 10;
                break;
            default:
                break;
        }
        
        //  3: Ancienneté du compte
        if (client.getCreatedAt() != null) {
            long months = java.time.temporal.ChronoUnit.MONTHS.between(
                client.getCreatedAt(), java.time.LocalDateTime.now());
            if (months > 12) {
                score += 10;
            } else if (months > 6) {
                score += 5;
            }
        }
        
        //  4: Statut du client
        switch (client.getStatus()) {
            case ACTIVE:
                score += 10;
                break;
            case SUSPENDED:
                score -= 20;
                break;
            case BLACKLISTED:
                score -= 50;
                break;
            default:
                break;
        }
        
        //  le score entre 0 et 100
        return Math.max(0, Math.min(100, score));
    }
    
    public CreditScoreResponse getCreditScoreResponse(Client client) {
        String rating;
        String recommendation;
        
        int score = client.getCreditScore();
        
        if (score >= 80) {
            rating = "EXCELLENT";
            recommendation = "Éligible à tous les produits de prêt. Taux préférentiels.";
        } else if (score >= 60) {
            rating = "BON";
            recommendation = "Éligible à la plupart des prêts. Taux standard.";
        } else if (score >= 40) {
            rating = "MOYEN";
            recommendation = "Éligible aux petits prêts. Améliorer le score en remboursant à temps.";
        } else if (score >= 20) {
            rating = "FAIBLE";
            recommendation = "Éligibilité limitée. Recommandé d'améliorer le comportement de paiement.";
        } else {
            rating = "CRITIQUE";
            recommendation = "Non éligible aux nouveaux prêts. Contacter un conseiller.";
        }
        
        return CreditScoreResponse.builder()
                .clientId(client.getId())
                .clientName(client.getFirstName() + " " + client.getLastName())
                .creditScore(score)
                .rating(rating)
                .recommendation(recommendation)
                .build();
    }
}