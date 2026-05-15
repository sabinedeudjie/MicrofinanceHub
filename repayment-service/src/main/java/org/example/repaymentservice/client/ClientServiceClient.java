package org.example.repaymentservice.client;

import org.example.repaymentservice.dto.ClientInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "CLIENT-SERVICE", url = "${client.service.url:http://localhost:8081}")
public interface ClientServiceClient {
    
    /**
     * Récupère les informations d'un client par son ID
     */
    @GetMapping("/api/clients/{clientId}")
    ClientInfo getClientInfo(
            @PathVariable("clientId") String clientId,
            @RequestHeader("Authorization") String authorization);
    
    /**
     * Récupère le nom d'un client (prénom + nom)
     */
    @GetMapping("/api/clients/{clientId}/name")
    String getClientName(
            @PathVariable("clientId") String clientId,
            @RequestHeader("Authorization") String authorization);

    /**
     * Récupère les clients d'un agent (pour l'agent connecté)
     * Utilise l'email de l'agent passé en header
     */
    @GetMapping("/api/clients/my-clients")
    List<ClientInfo> getMyClients(
            @RequestHeader("X-User-Id") String agentId,
            @RequestHeader("Authorization") String authorization);
    
    /**
     * Compte les clients d'un agent
     */
    @GetMapping("/api/clients/my-clients/count")
    Long countMyClients(
            @RequestHeader("X-User-Id") String agentId,
            @RequestHeader("Authorization") String authorization);
    
    /**
     * Récupère les clients d'un agent (par ID de l'agent)
     */
    @GetMapping("/api/clients/by-agent/{agentId}")
    List<ClientInfo> getClientsByAgent(
            @PathVariable("agentId") String agentId,
            @RequestHeader("Authorization") String authorization);
    
    /**
     * Récupère les informations d'un client par email
     */
    @GetMapping("/api/clients/by-email-exact")
    ClientInfo getClientInfoByEmail(
            @RequestParam("email") String email,
            @RequestHeader("Authorization") String authorization);
    
    /**
     *  Récupère les IDs des clients d'une agence
     * Utilisé par Agency Service
     */
    @GetMapping("/api/clients/by-agency/{agencyId}/ids")
    List<String> getClientIdsByAgency(
            @PathVariable("agencyId") String agencyId,
            @RequestHeader("Authorization") String authorization);
    
    /**
     *  Vérifie si un client existe par ID
     */
    @GetMapping("/api/clients/{clientId}/exists")
    Boolean clientExists(
            @PathVariable("clientId") String clientId,
            @RequestHeader("Authorization") String authorization);
}