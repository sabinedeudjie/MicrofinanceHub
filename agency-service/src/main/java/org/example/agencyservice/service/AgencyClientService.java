package org.example.agencyservice.service;

import org.example.agencyservice.client.AccountServiceClient;
import org.example.agencyservice.client.ClientServiceClient;
import org.example.agencyservice.dto.response.AgencyAccountInfo;
import org.example.agencyservice.dto.response.AgencyClientInfo;
import org.example.agencyservice.dto.response.AgencyClientsResponse;
import org.example.agencyservice.dto.response.ClientInfo;
import org.example.agencyservice.model.Agency;
import org.example.agencyservice.model.AgentAssignment;
import org.example.agencyservice.repository.AgencyRepository;
import org.example.agencyservice.repository.AgentAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgencyClientService {
    
    private final AgencyRepository agencyRepository;
    private final AgentAssignmentRepository agentAssignmentRepository;
    private final ClientServiceClient clientServiceClient;
    private final AccountServiceClient accountServiceClient;
    
    
    /**
     * Récupère tous les clients d'une agence via les emails des agents assignés.
     * Utilise un endpoint interne (sans auth) pour éviter les problèmes d'encodage URL.
     */
    public AgencyClientsResponse getAgencyClientsWithAccounts(String agencyId, String token) {
        log.info("Chargement des clients pour l'agence: {}", agencyId);

        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new RuntimeException("Agence non trouvée: " + agencyId));

        // 1. Récupérer tous les agents actifs de l'agence
        List<AgentAssignment> assignments = agentAssignmentRepository.findByAgencyIdAndActiveTrue(agencyId);
        if (assignments.isEmpty()) {
            log.warn("Aucun agent actif pour l'agence: {}", agencyId);
            return buildEmptyResponse(agency);
        }

        List<String> agentEmails = assignments.stream()
                .map(AgentAssignment::getAgentEmail)
                .filter(e -> e != null && !e.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        log.info("Agents trouvés pour l'agence {}: {}", agencyId, agentEmails);

        // 2. Appel interne client-service : tous les clients de ces agents en un seul appel
        Map<String, ClientInfo> uniqueClients = new LinkedHashMap<>();
        try {
            List<ClientInfo> found = clientServiceClient.getClientsByAgentEmails(agentEmails);
            if (found != null) {
                for (ClientInfo c : found) {
                    if (c.getId() != null) uniqueClients.putIfAbsent(c.getId(), c);
                }
            }
        } catch (Exception e) {
            log.error("Erreur appel interne clients: {}", e.getMessage(), e);
            return buildEmptyResponse(agency);
        }

        log.info("Clients trouvés pour l'agence {}: {}", agencyId, uniqueClients.size());

        if (uniqueClients.isEmpty()) {
            return buildEmptyResponse(agency);
        }

        // 3. Pour chaque client unique, récupérer ses comptes
        List<AgencyClientInfo> clientInfos = new ArrayList<>();
        BigDecimal totalBalance = BigDecimal.ZERO;
        int totalAccounts = 0;

        for (ClientInfo client : uniqueClients.values()) {
            try {
                List<AgencyAccountInfo> accounts = new ArrayList<>();
                try {
                    accounts = accountServiceClient.getClientAccounts(client.getId(), token);
                    if (accounts == null) accounts = new ArrayList<>();
                } catch (Exception e) {
                    log.warn("Erreur comptes client {}: {}", client.getId(), e.getMessage());
                }

                BigDecimal clientBalance = accounts.stream()
                        .map(AgencyAccountInfo::getBalance)
                        .filter(b -> b != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                clientInfos.add(AgencyClientInfo.builder()
                        .clientId(client.getId())
                        .clientEmail(client.getEmail())
                        .clientFirstName(client.getFirstName())
                        .clientLastName(client.getLastName())
                        .clientPhone(client.getPhoneNumber())
                        .clientCreatedAt(client.getCreatedAt())
                        .clientStatus(client.getStatus())
                        .clientCreditScore(client.getCreditScore())
                        .clientCreatedBy(client.getCreatedBy())
                        .accounts(accounts)
                        .totalAccounts(accounts.size())
                        .totalBalance(clientBalance)
                        .build());

                totalBalance = totalBalance.add(clientBalance);
                totalAccounts += accounts.size();
            } catch (Exception e) {
                log.error("Erreur traitement client {}: {}", client.getId(), e.getMessage());
            }
        }

        return AgencyClientsResponse.builder()
                .agencyId(agency.getId())
                .agencyCode(agency.getCode())
                .agencyName(agency.getName())
                .totalClients(clientInfos.size())
                .totalAccounts(totalAccounts)
                .totalBalance(totalBalance)
                .clients(clientInfos)
                .build();
    }
    /**
     * Vérifie si un client appartient à une agence
     */

    public boolean validateClientBelongsToAgency(String agencyId, String clientId, String clientEmail, String token) {
        log.info("Validation client {} / email {} pour agence {}", clientId, clientEmail, agencyId);
        try {
            List<AgentAssignment> assignments = agentAssignmentRepository.findByAgencyIdAndActiveTrue(agencyId);
            List<String> agentEmails = assignments.stream()
                    .map(AgentAssignment::getAgentEmail)
                    .filter(e -> e != null && !e.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            if (agentEmails.isEmpty()) return false;

            List<ClientInfo> clients = clientServiceClient.getClientsByAgentEmails(agentEmails);
            if (clients == null) return false;

            if (clientId != null && !clientId.isEmpty()) {
                return clients.stream().anyMatch(c -> clientId.equals(c.getId()));
            } else if (clientEmail != null && !clientEmail.isEmpty()) {
                return clients.stream().anyMatch(c -> clientEmail.equals(c.getEmail()));
            }
            return false;
        } catch (Exception e) {
            log.error("Erreur validation: {}", e.getMessage(), e);
            return false;
        }
    }
    /**
     * Récupère les statistiques des clients d'une agence
     */
    public Map<String, Object> getAgencyClientsStats(String agencyId, String token) {
        log.info("des clients pour l'agence: {}", agencyId);
        
        AgencyClientsResponse clients = getAgencyClientsWithAccounts(agencyId, token);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("agencyId", agencyId);
        stats.put("agencyCode", clients.getAgencyCode());
        stats.put("agencyName", clients.getAgencyName());
        stats.put("totalClients", clients.getTotalClients());
        stats.put("totalAccounts", clients.getTotalAccounts());
        stats.put("totalBalance", clients.getTotalBalance());
        
        //  la moyenne de comptes par client
        double avgAccountsPerClient = clients.getTotalClients() > 0 ? 
            (double) clients.getTotalAccounts() / clients.getTotalClients() : 0;
        stats.put("avgAccountsPerClient", Math.round(avgAccountsPerClient * 100.0) / 100.0);
        
        //  la balance moyenne par client
        double avgBalancePerClient = clients.getTotalClients() > 0 && clients.getTotalBalance() != null ?
            clients.getTotalBalance().doubleValue() / clients.getTotalClients() : 0;
        stats.put("avgBalancePerClient", Math.round(avgBalancePerClient * 100.0) / 100.0);
        
        //  des types de comptes
        Map<String, Integer> accountTypeDistribution = new HashMap<>();
        for (AgencyClientInfo client : clients.getClients()) {
            if (client.getAccounts() == null) continue;
            for (AgencyAccountInfo account : client.getAccounts()) {
                String type = account.getAccountType();
                if (type != null) {
                    accountTypeDistribution.put(type, accountTypeDistribution.getOrDefault(type, 0) + 1);
                }
            }
        }
        stats.put("accountTypeDistribution", accountTypeDistribution);
        
        return stats;
    }
    
    private AgencyClientsResponse buildEmptyResponse(Agency agency) {
        return AgencyClientsResponse.builder()
                .agencyId(agency.getId())
                .agencyCode(agency.getCode())
                .agencyName(agency.getName())
                .totalClients(0)
                .totalAccounts(0)
                .totalBalance(BigDecimal.ZERO)
                .clients(new ArrayList<>())
                .build();
    }
}