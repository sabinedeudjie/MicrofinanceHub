package org.example.transactionservice.campay;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionservice.campay.dto.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class CamPayService {

    private final CamPayProperties properties;

    private static final String DEVISE_XAF = "XAF";
    private static final RestClient REST_CLIENT = RestClient.create();

    private String url(String path) {
        String base = properties.getBase().getUrl().replaceAll("/+$", "");
        log.info("URL → {}{}", base, path);
        return base + path;
    }

    public String getToken() {
        String token = properties.getApp().getPermanentAccessToken();
        if (token == null || token.isBlank()) {
            throw new CamPayException("Token permanent CamPay non configuré (campay.app.permanent-access-token)");
        }
        return token;
    }

    public CamPayCollectResponse initierCollecte(
            String numeroPaiement, BigDecimal montant,
            String referenceInterne, String description) {

        log.info("collecte CamPay : {} XAF depuis {} (réf: {})", montant, numeroPaiement, referenceInterne);

        CamPayCollectRequest request = CamPayCollectRequest.builder()
                .amount(montant.toBigInteger().toString())
                .currency(DEVISE_XAF)
                .from(numeroPaiement)
                .description(description)
                .externalReference(referenceInterne)
                .build();

        try {
            CamPayCollectResponse response = REST_CLIENT
                    .post()
                    .uri(url("/collect/"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Token " + getToken())
                    .body(request)
                    .retrieve()
                    .body(CamPayCollectResponse.class);

            if (response == null || response.getReference() == null) {
                throw new CamPayException("Réponse CamPay invalide lors de l'initiation de la collecte");
            }

            log.info("initiée (réf CamPay: {}, statut: {})", response.getReference(), response.getStatus());
            return response;

        } catch (RestClientException e) {
            log.error("HTTP CamPay collecte : {}", e.getMessage());
            throw new CamPayException("Impossible de contacter CamPay : " + e.getMessage(), e);
        }
    }

    public CamPayDisburseResponse effectuerDecaissement(
            String numeroBeneficiaire, BigDecimal montant,
            String referenceInterne, String description) {

        log.info("CamPay : {} XAF vers {} (réf: {})", montant, numeroBeneficiaire, referenceInterne);

        CamPayDisburseRequest request = CamPayDisburseRequest.builder()
                .amount(montant.toBigInteger().toString())
                .currency(DEVISE_XAF)
                .to(numeroBeneficiaire)
                .description(description)
                .externalReference(referenceInterne)
                .build();

        try {
            CamPayDisburseResponse response = REST_CLIENT
                    .post()
                    .uri(url("/withdraw/"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Token " + getToken())
                    .body(request)
                    .retrieve()
                    .body(CamPayDisburseResponse.class);

            if (response == null || response.getReference() == null) {
                throw new CamPayException("Réponse CamPay invalide lors du décaissement");
            }

            log.info("initié (réf CamPay: {}, statut: {})", response.getReference(), response.getStatus());
            return response;

        } catch (RestClientException e) {
            log.error("HTTP CamPay décaissement : {}", e.getMessage());
            throw new CamPayException("Impossible de contacter CamPay pour le décaissement : " + e.getMessage(), e);
        }
    }

    public CamPayTransactionStatusResponse verifierStatut(String campayReference) {
        log.debug("statut CamPay : {}", campayReference);

        try {
            CamPayTransactionStatusResponse response = REST_CLIENT
                    .get()
                    .uri(url("/transaction/" + campayReference + "/"))
                    .header("Authorization", "Token " + getToken())
                    .retrieve()
                    .body(CamPayTransactionStatusResponse.class);

            if (response == null) {
                throw new CamPayException("Aucune réponse de CamPay pour la référence : " + campayReference);
            }

            return response;

        } catch (RestClientException e) {
            log.error("HTTP CamPay statut : {}", e.getMessage());
            throw new CamPayException("Impossible de vérifier le statut CamPay : " + e.getMessage(), e);
        }
    }
}
