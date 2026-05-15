package org.example.authservice.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientServiceClient {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${client.service.url:http://localhost:8081}")
    private String clientServiceUrl;
    
    /**
     * Vérifie si un client existe par email
     * Utilisé lors de l'inscription pour valider que le client est bien dans la base Client Service
     */
    public boolean clientExistsByEmail(String email) {
        log.info("Vérification de l'existence du client par email: {}", email);
        
        try {
            Boolean exists = webClient.get()
                    .uri(clientServiceUrl + "/api/clients/exists/by-email?email={email}", email)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            boolean result = Boolean.TRUE.equals(exists);
            log.info("existe pour {}: {}", email, result);
            return result;
            
        } catch (WebClientResponseException.NotFound e) {
            log.warn("non trouvé pour: {}", email);
            return false;
        } catch (Exception e) {
            log.error("lors de la vérification du client: {}", e.getMessage());
            //  cas d'erreur technique, on refuse l'inscription par sécurité
            return false;
        }
    }
    
    /**
     * Récupère l'ID du client par email
     */
    public String getClientIdByEmail(String email, String token) {
        log.info("Recherche de l'ID du client par email: {}", email);
        
        try {
            String response = webClient.get()
                    .uri(clientServiceUrl + "/api/clients/by-email-exact?email={email}", email)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            if (response != null && response.contains("\"id\"")) {
                JsonNode jsonNode = objectMapper.readTree(response);
                return jsonNode.get("id").asText();
            }
        } catch (Exception e) {
            log.error("lors de la récupération du client: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Récupère les détails complets d'un client par email
     */
    public JsonNode getClientByEmail(String email, String token) {
        log.info("Recherche du client par email: {}", email);
        
        try {
            String response = webClient.get()
                    .uri(clientServiceUrl + "/api/clients/by-email-exact?email={email}", email)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            if (response != null) {
                return objectMapper.readTree(response);
            }
        } catch (Exception e) {
            log.error("lors de la récupération du client: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Récupère les données complètes d'un client par email sans authentification (endpoint public)
     */
    public JsonNode getClientByEmailPublic(String email) {
        log.info("Récupération publique du client par email: {}", email);
        try {
            String response = webClient.get()
                    .uri(clientServiceUrl + "/api/clients/public/by-email?email={email}", email)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            if (response != null) {
                return objectMapper.readTree(response);
            }
        } catch (Exception e) {
            log.error("lors de la récupération publique du client: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Met à jour la dernière connexion d'un client
     */
    public void updateLastLogin(String clientId, String token) {
        log.info("Mise à jour de la dernière connexion pour le client: {}", clientId);
        
        try {
            webClient.patch()
                    .uri(clientServiceUrl + "/api/clients/{id}/last-login", clientId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            log.info("Dernière connexion mise à jour pour le client: {}", clientId);
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour: {}", e.getMessage());
        }
    }
    
    /**
     * Met à jour la dernière connexion d'un client par email
     */
    public void updateLastLoginByEmail(String email, String token) {
        log.info("Mise à jour de la dernière connexion pour l'email: {}", email);
        
        try {
            webClient.patch()
                    .uri(clientServiceUrl + "/api/clients/update-last-login-by-email?email={email}", email)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            log.info("Dernière connexion mise à jour pour: {}", email);
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour: {}", e.getMessage());
        }
    }
}