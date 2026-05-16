package org.example.transactionservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionservice.config.JwtUtil;
import org.example.transactionservice.dto.CompteInfo;
import org.example.transactionservice.exception.CompteIndisponibleException;
import org.example.transactionservice.exception.OperationInvalideException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * Client HTTP vers account-service.
 * Utilise un token JWT interne ADMIN généré localement (même secret partagé).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CompteServiceClient {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Value("${account.service.url:http://localhost:8082}")
    private String accountServiceUrl;

    private static final RestClient REST_CLIENT = RestClient.create();

    public CompteInfo getCompteById(Long compteId) {
        try {
            String json = REST_CLIENT.get()
                    .uri(accountServiceUrl + "/api/comptes/" + compteId)
                    .header("Authorization", "Bearer " + jwtUtil.generateServiceToken())
                    .retrieve()
                    .body(String.class);

            return parseCompteInfo(json);

        } catch (HttpClientErrorException.NotFound e) {
            throw new CompteIndisponibleException(compteId);
        } catch (CompteIndisponibleException e) {
            throw e;
        } catch (Exception e) {
            log.error("Impossible de recuperer le compte {} depuis account-service : {}", compteId, e.getMessage());
            throw new OperationInvalideException("Compte service indisponible : " + e.getMessage());
        }
    }

    public CompteInfo getCompteByNumero(String numeroCompte) {
        try {
            String json = REST_CLIENT.get()
                    .uri(accountServiceUrl + "/api/comptes/numero/" + numeroCompte)
                    .header("Authorization", "Bearer " + jwtUtil.generateServiceToken())
                    .retrieve()
                    .body(String.class);

            return parseCompteInfo(json);

        } catch (HttpClientErrorException.NotFound e) {
            throw new CompteIndisponibleException(numeroCompte);
        } catch (CompteIndisponibleException e) {
            throw e;
        } catch (Exception e) {
            log.error("Impossible de recuperer le compte {} : {}", numeroCompte, e.getMessage());
            throw new OperationInvalideException("Compte service indisponible : " + e.getMessage());
        }
    }

    public CompteInfo getClientCompteInfo(String clientId) {
        try {
            String json = REST_CLIENT.get()
                    .uri(accountServiceUrl + "/api/internal/accounts/client/" + clientId + "/all")
                    .header("Authorization", "Bearer " + jwtUtil.generateServiceToken())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(json);
            if (root.isArray() && root.size() > 0) {
                JsonNode firstCompte = root.get(0);
                CompteInfo info = new CompteInfo();
                info.setClientId(Long.parseLong(clientId));
                
                if (firstCompte.has("clientEmail") && !firstCompte.get("clientEmail").isNull()) {
                    info.setClientEmail(firstCompte.get("clientEmail").asText());
                }
                if (firstCompte.has("clientNom") && !firstCompte.get("clientNom").isNull()) {
                    info.setClientNom(firstCompte.get("clientNom").asText());
                }
                
                return info;
            }
            return null;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des infos client {}: {}", clientId, e.getMessage());
            return null;
        }
    }

    public void crediterCompte(Long compteId, BigDecimal montant) {
        try {
            REST_CLIENT.post()
                    .uri(accountServiceUrl + "/api/comptes/" + compteId + "/crediter?montant=" + montant.toPlainString())
                    .header("Authorization", "Bearer " + jwtUtil.generateServiceToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity();

            log.info("{} crédité de {} XAF via account-service", compteId, montant);

        } catch (Exception e) {
            log.error("lors du crédit du compte {} : {}", compteId, e.getMessage());
            throw new OperationInvalideException("Impossible de créditer le compte : " + e.getMessage());
        }
    }

    public void debiterCompte(Long compteId, BigDecimal montant) {
        try {
            REST_CLIENT.post()
                    .uri(accountServiceUrl + "/api/comptes/" + compteId + "/debiter?montant=" + montant.toPlainString())
                    .header("Authorization", "Bearer " + jwtUtil.generateServiceToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity();

            log.info("{} débité de {} XAF via account-service", compteId, montant);

        } catch (Exception e) {
            log.error("lors du débit du compte {} : {}", compteId, e.getMessage());
            throw new OperationInvalideException("Impossible de débiter le compte : " + e.getMessage());
        }
    }

    private CompteInfo parseCompteInfo(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.path("data");

            CompteInfo info = new CompteInfo();
            info.setId(data.path("id").asLong());
            info.setClientId(data.path("clientId").asLong());
            info.setNumeroCompte(data.path("numeroCompte").asText());
            info.setSolde(new BigDecimal(data.path("solde").asText("0")));
            info.setStatut(data.path("statut").asText());
            info.setTypeCompte(data.path("typeCompte").asText());

            JsonNode email = data.path("clientEmail");
            if (!email.isNull() && !email.isMissingNode()) info.setClientEmail(email.asText());

            JsonNode nom = data.path("clientNom");
            if (!nom.isNull() && !nom.isMissingNode()) info.setClientNom(nom.asText());

            JsonNode plafond = data.path("plafond");
            if (!plafond.isNull() && !plafond.isMissingNode()) {
                info.setPlafond(new BigDecimal(plafond.asText("0")));
            }

            JsonNode soldeMin = data.path("soldeMinimum");
            if (!soldeMin.isNull() && !soldeMin.isMissingNode()) {
                info.setSoldeMinimum(new BigDecimal(soldeMin.asText("0")));
            }

            return info;

        } catch (Exception e) {
            log.error("parsing CompteInfo : {}", e.getMessage());
            throw new OperationInvalideException("Réponse account-service invalide");
        }
    }
}
