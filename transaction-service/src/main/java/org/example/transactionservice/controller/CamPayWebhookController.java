package org.example.transactionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionservice.campay.CamPayProperties;
import org.example.transactionservice.campay.dto.CamPayWebhookPayload;
import org.example.transactionservice.model.TypeTransaction;
import org.example.transactionservice.repository.TransactionRepository;
import org.example.transactionservice.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/campay")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhook CamPay", description = "Réception des notifications CamPay Mobile Money")
@SecurityRequirements
public class CamPayWebhookController {

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final CamPayProperties camPayProperties;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Recevoir une notification CamPay")
    @PostMapping("/webhook")
    public ResponseEntity<Void> recevoirWebhook(
            @RequestHeader(value = "Webhook-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        log.info("CamPay reçu (taille: {} octets)", rawBody.length());

        if (signature != null && !verifierSignature(rawBody, signature)) {
            log.warn("CamPay rejeté : signature invalide");
            return ResponseEntity.status(401).build();
        }

        CamPayWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, CamPayWebhookPayload.class);
        } catch (Exception e) {
            log.error("CamPay : impossible de désérialiser le payload : {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        log.info("CamPay : réf={}, statut={}, external_ref={}",
                payload.getReference(), payload.getStatus(), payload.getExternalReference());

        transactionRepository.findByCampayReference(payload.getReference())
                .or(() -> payload.getExternalReference() != null
                        ? transactionRepository.findByReference(payload.getExternalReference())
                        : java.util.Optional.empty())
                .ifPresentOrElse(
                        transaction -> {
                            if (TypeTransaction.DEPOT.equals(transaction.getTypeTransaction())) {
                                transactionService.traiterWebhookDepot(payload);
                            } else if (TypeTransaction.RETRAIT.equals(transaction.getTypeTransaction())) {
                                transactionService.traiterWebhookRetrait(payload);
                            } else if (TypeTransaction.REMBOURSEMENT_PRET.equals(transaction.getTypeTransaction())) {
                                transactionService.traiterWebhookRemboursement(payload);
                            }
                        },
                        () -> log.warn("CamPay : aucune transaction trouvée (réf={}, external_ref={})",
                                payload.getReference(), payload.getExternalReference())
                );

        return ResponseEntity.ok().build();
    }

    private boolean verifierSignature(String corps, String signature) {
        try {
            String webhookKey = camPayProperties.getApp().getWebhook().getKey();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmac = mac.doFinal(corps.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hmac);
            return expected.equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("vérification signature webhook : {}", e.getMessage());
            return false;
        }
    }
}
