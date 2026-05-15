package org.example.clientservice.service;

import org.example.clientservice.client.AuthServiceClient;
import org.example.clientservice.config.RabbitMQConfig;
import org.example.clientservice.dto.UserDTO;
import org.example.clientservice.dto.event.ClientCreatedEvent;
import org.example.clientservice.dto.request.ClientRequest;
import org.example.clientservice.dto.request.ClientSearchRequest;
import org.example.clientservice.dto.response.ClientResponse;
import org.example.clientservice.dto.response.ClientStatsResponse;
import org.example.clientservice.dto.response.CreditScoreResponse;
import org.example.clientservice.dto.response.PageResponse;
import org.example.clientservice.exception.ClientNotFoundException;
import org.example.clientservice.exception.DuplicateEmailException;
import org.example.clientservice.exception.DuplicatePhoneException;
import org.example.clientservice.exception.InvalidClientDataException;
import org.example.clientservice.model.Client;
import org.example.clientservice.model.enums.ClientStatus;
import org.example.clientservice.model.enums.ClientType;
import org.example.clientservice.repository.ClientRepository;
import org.example.clientservice.specification.ClientSpecification;
import org.springframework.data.jpa.domain.Specification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {
    
    private final ClientRepository clientRepository;
    private final CreditScoreService creditScoreService;
    private final AuthServiceClient authServiceClient;
    private final RabbitTemplate rabbitTemplate;
    
    // 
    //  DE BASE
    // 
    
    @Transactional
    public ClientResponse createClient(ClientRequest request, String createdBy) {
        log.info("d'un nouveau client: {}", request.getEmail());
        
        //  si l'email existe déjà
        if (clientRepository.existsByEmail(request.getEmail())) {
            log.warn("de création avec email existant: {}", request.getEmail());
            throw new DuplicateEmailException("Email déjà utilisé: " + request.getEmail());
        }
        
        //  si le téléphone existe déjà
        if (request.getPhoneNumber() != null && clientRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            log.warn("de création avec téléphone existant: {}", request.getPhoneNumber());
            throw new DuplicatePhoneException("Numéro de téléphone déjà utilisé: " + request.getPhoneNumber());
        }
        
        //  le client
        Client client = Client.builder()
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .address(request.getAddress())
                .birthDate(request.getBirthDate())
                .clientType(request.getClientType() != null ? request.getClientType() : ClientType.INDIVIDUAL)
                .status(ClientStatus.ACTIVE)
                .creditScore(50)
                .createdBy(createdBy)
                .agencyId(request.getAgencyId())
                .build();
        
        client = clientRepository.save(client);

        //  le score de crédit initial
        creditScoreService.updateCreditScore(client.getId());

        // Publier l'événement de bienvenue vers notification-service
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CLIENT_EXCHANGE,
                RabbitMQConfig.CLIENT_CREATED_KEY,
                ClientCreatedEvent.builder()
                    .clientId(client.getId())
                    .firstName(client.getFirstName())
                    .lastName(client.getLastName())
                    .email(client.getEmail())
                    .phoneNumber(client.getPhoneNumber())
                    .createdBy(createdBy)
                    .build()
            );
        } catch (Exception e) {
            log.warn("Impossible de publier l'événement client.created pour {} : {}", client.getEmail(), e.getMessage());
        }

        log.info("créé avec succès: {} - ID: {}", client.getEmail(), client.getId());

        return mapToResponse(client);
    }
    
    @Transactional(readOnly = true)
    public ClientResponse getClient(String id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Client non trouvé avec l'ID: " + id));
        return mapToResponse(client);
    }
    
    @Transactional(readOnly = true)
    public ClientResponse getClientByEmail(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new ClientNotFoundException("Client non trouvé avec l'email: " + email));
        return mapToResponse(client);
    }
    
    @Transactional(readOnly = true)
    public List<ClientResponse> getAllClients() {
        return clientRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ClientResponse> getClientsByStatus(ClientStatus status) {
        return clientRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public ClientResponse updateClient(String id, ClientRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Client non trouvé avec l'ID: " + id));
        
        //  à jour les champs
        if (request.getEmail() != null && !request.getEmail().equals(client.getEmail())) {
            if (clientRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateEmailException("Email déjà utilisé: " + request.getEmail());
            }
            client.setEmail(request.getEmail());
        }
        
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(client.getPhoneNumber())) {
            if (clientRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new DuplicatePhoneException("Numéro de téléphone déjà utilisé: " + request.getPhoneNumber());
            }
            client.setPhoneNumber(request.getPhoneNumber());
        }
        
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new InvalidClientDataException("Le prénom est obligatoire");
        }
        
        client.setFirstName(request.getFirstName());
        client.setLastName(request.getLastName());
        client.setAddress(request.getAddress());
        client.setBirthDate(request.getBirthDate());
        
        if (request.getClientType() != null) {
            client.setClientType(request.getClientType());
        }
        
        client = clientRepository.save(client);
        
        log.info("mis à jour: {}", client.getId());
        
        return mapToResponse(client);
    }
    
    @Transactional
    public void deleteClient(String id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Client non trouvé avec l'ID: " + id));
        
        client.setStatus(ClientStatus.INACTIVE);
        clientRepository.save(client);
        
        log.info("désactivé: {}", id);
    }
    
    @Transactional
    public ClientResponse updateClientStatus(String id, ClientStatus status) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Client non trouvé avec l'ID: " + id));
        
        client.setStatus(status);
        client = clientRepository.save(client);
        
        log.info("du client {} mis à jour: {}", client.getId(), status);
        
        return mapToResponse(client);
    }
    
    @Transactional(readOnly = true)
    public CreditScoreResponse getCreditScore(String id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Client non trouvé avec l'ID: " + id));
        
        return creditScoreService.getCreditScoreResponse(client);
    }

    // 
    //  DE RECHERCHE
    // 
    
    @Transactional(readOnly = true)
    public ClientResponse findByEmailExact(String email) {
        Client client = clientRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ClientNotFoundException("Client non trouvé avec l'email: " + email));
        return mapToResponse(client);
    }

    private String normalizePhoneNumber(String phone) {
        if (phone == null) return null;
        phone = phone.replaceAll("\\s+", "").replaceAll("-", "");
        if (!phone.startsWith("+") && phone.matches("\\d+")) {
            phone = "+" + phone;
        }
        return phone;
    }   
    
    @Transactional(readOnly = true)
    public ClientResponse findByPhoneExact(String phone) {
        String normalizedPhone = normalizePhoneNumber(phone);
        return clientRepository.findByPhoneNumber(normalizedPhone)
            .map(this::mapToResponse)
            .orElseThrow(() -> new ClientNotFoundException("Client non trouvé avec le téléphone: " + phone));
    }
    
    @Transactional(readOnly = true)
    public PageResponse<ClientResponse> findByStatusPaginated(ClientStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Client> clientPage = clientRepository.findByStatus(status, pageable);
        
        List<ClientResponse> content = clientPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return PageResponse.<ClientResponse>builder()
                .content(content)
                .pageNumber(clientPage.getNumber())
                .pageSize(clientPage.getSize())
                .totalElements(clientPage.getTotalElements())
                .totalPages(clientPage.getTotalPages())
                .first(clientPage.isFirst())
                .last(clientPage.isLast())
                .empty(clientPage.isEmpty())
                .build();
    }
    
    @Transactional(readOnly = true)
    public List<ClientResponse> findByClientType(ClientType clientType) {
        return clientRepository.findByClientType(clientType).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ClientResponse> findByMinScore(Integer minScore) {
        return clientRepository.findByCreditScoreGreaterThanEqual(minScore).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ClientResponse> findByMaxScore(Integer maxScore) {
        return clientRepository.findByCreditScoreLessThanEqual(maxScore).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ClientResponse> findByScoreBetween(Integer minScore, Integer maxScore) {
        return clientRepository.findByCreditScoreBetween(minScore, maxScore).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ClientResponse> findByCreatedAfter(LocalDateTime createdAfter) {
        return clientRepository.findByCreatedAtAfter(createdAfter).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ClientResponse> searchGlobal(String searchTerm) {
        if (!StringUtils.hasText(searchTerm)) {
            return getAllClients();
        }
        return clientRepository.searchGlobal(searchTerm).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public PageResponse<ClientResponse> searchGlobalPaginated(String searchTerm, int page, int size) {
        if (!StringUtils.hasText(searchTerm)) {
            return getAllClientsPaginated(page, size);
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Client> clientPage = clientRepository.searchGlobalPaginated(searchTerm, pageable);
        
        List<ClientResponse> content = clientPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return PageResponse.<ClientResponse>builder()
                .content(content)
                .pageNumber(clientPage.getNumber())
                .pageSize(clientPage.getSize())
                .totalElements(clientPage.getTotalElements())
                .totalPages(clientPage.getTotalPages())
                .first(clientPage.isFirst())
                .last(clientPage.isLast())
                .empty(clientPage.isEmpty())
                .build();
    }
    
    @Transactional(readOnly = true)
    public List<ClientResponse> searchWithFilters(ClientSearchRequest filters) {
        Specification<Client> spec = ClientSpecification.searchWithFilters(
                filters.getEmail(),
                filters.getPhone(),
                filters.getStatus(),
                filters.getClientType(),
                filters.getMinScore(),
                filters.getMaxScore(),
                filters.getCreatedAfter(),
                filters.getSearch()
        );
        
        List<Client> clients = clientRepository.findAll(spec);
        return clients.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public PageResponse<ClientResponse> getAllClientsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Client> clientPage = clientRepository.findAll(pageable);
        
        List<ClientResponse> content = clientPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return PageResponse.<ClientResponse>builder()
                .content(content)
                .pageNumber(clientPage.getNumber())
                .pageSize(clientPage.getSize())
                .totalElements(clientPage.getTotalElements())
                .totalPages(clientPage.getTotalPages())
                .first(clientPage.isFirst())
                .last(clientPage.isLast())
                .empty(clientPage.isEmpty())
                .build();
    }

    @Transactional
    public ClientResponse updateLastLogin(String id) {
        log.info(".updateLastLogin() - DÉBUT pour ID: {}", id);
        
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("non trouvé avec l'ID: {}", id);
                    return new ClientNotFoundException("Client non trouvé avec l'ID: " + id);
                });
        
        log.info("   Client trouvé: {}", client.getEmail());
        log.info("   Ancien lastLoginAt: {}", client.getLastLoginAt());
        
        client.setLastLoginAt(LocalDateTime.now());
        client = clientRepository.save(client);
        
        log.info("   Nouveau lastLoginAt: {}", client.getLastLoginAt());
        log.info(".updateLastLogin() - FIN pour ID: {}", id);
        
        return mapToResponse(client);
    }


     @Transactional
    public ClientResponse updateLastLoginByEmail(String email) {
    log.info("à jour de la dernière connexion pour: {}", email);
    
           Optional<Client> clientOpt = clientRepository.findByEmail(email);
          if (clientOpt.isEmpty()) {
             log.warn("non trouvé avec l'email: {}, mise à jour ignorée", email);
             return null; //  retourner null sans erreur
           }
    
           Client client = clientOpt.get();
           client.setLastLoginAt(LocalDateTime.now());
           client = clientRepository.save(client);
    
           log.info("connexion mise à jour pour: {}", email);
    
           return mapToResponse(client);
    }

    @Transactional(readOnly = true)
    public boolean clientExists(String id) {
        log.info("de l'existence du client avec ID: {}", id);
        return clientRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public boolean clientExistsByEmail(String email) {
        log.info("de l'existence du client avec email: {}", email);
        return clientRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean clientExistsByPhone(String phone) {
        log.info("de l'existence du client avec téléphone: {}", phone);
        String normalizedPhone = normalizePhoneNumber(phone);
        return clientRepository.existsByPhoneNumber(normalizedPhone);
    }

    /**
    * Vérifie si un client existe par email
    */
   @Transactional(readOnly = true)
   public boolean existsByEmail(String email) {
       log.info("de l'existence du client avec email: {}", email);
       return clientRepository.existsByEmail(email);
    }
    
    /**
    * Statistique client
    */
    public ClientStatsResponse getClientStats() {
    log.info("des statistiques clients");
    
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0);
    LocalDateTime startOfLastMonth = startOfMonth.minusMonths(1);
    LocalDateTime endOfLastMonth = startOfMonth.minusDays(1);
    
    //  des clients
    Long totalClients = clientRepository.count();
    
    //  actifs (status = ACTIVE)
    Long activeClients = clientRepository.countByStatus(ClientStatus.ACTIVE);
    
    //  clients du mois
    Long newClientsThisMonth = clientRepository.countByCreatedAtBetween(startOfMonth, now);
    
    //  clients du mois dernier (pour calculer la croissance)
    Long newClientsLastMonth = clientRepository.countByCreatedAtBetween(startOfLastMonth, endOfLastMonth);
    
    //  le taux de croissance
    Double clientGrowthRate = calculateGrowthRate(newClientsThisMonth, newClientsLastMonth);
    
    //  inactifs
    Long inactiveClients = clientRepository.countByStatus(ClientStatus.INACTIVE);
    
    //  en attente
    Long pendingClients = clientRepository.countByStatus(ClientStatus.PENDING);
    
    return ClientStatsResponse.builder()
        .totalClients(totalClients)
        .activeClients(activeClients)
        .inactiveClients(inactiveClients)
        .pendingClients(pendingClients)
        .newClientsThisMonth(newClientsThisMonth)
        .clientGrowthRate(clientGrowthRate)
        .build();
}
    
    private Double calculateGrowthRate(Long current, Long previous) {
        if (previous == null || previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((double) (current - previous) / previous) * 100;
    }

     /**
     * Récupère tous les clients associés à un agent
     * @param agentId L'identifiant de l'agent
     * @return Liste des clients de l'agent
     */
    /**
     * Récupère l'email d'un agent à partir de son UUID
     * @param agentId L'UUID de l'agent
     * @param token Le token JWT pour l'authentification
     * @return L'email de l'agent ou null si non trouvé
     */
    private String getAgentEmailById(String agentId, String token) {
        if (authServiceClient == null || token == null) {
            log.warn("ou token null, impossible de résoudre l'UUID: {}", agentId);
            return null;
        }
        
        try {
            //  le token (enlever "Bearer " si présent)
            String cleanToken = token.startsWith("Bearer ") ? token : "Bearer " + token;
            
            log.info("à Auth Service pour résoudre l'UUID: {}", agentId);
            UserDTO user = authServiceClient.getUserById(agentId, cleanToken);
            
            if (user != null && user.getEmail() != null) {
                log.info("{} résolu en email: {}", agentId, user.getEmail());
                return user.getEmail();
            } else {
                log.warn("utilisateur trouvé pour l'UUID: {}", agentId);
            }
        } catch (Exception e) {
            log.error("lors de la résolution de l'UUID {}: {}", agentId, e.getMessage());
        }
        
        return null;
    }
    @Transactional(readOnly = true)
public List<ClientResponse> getClientsByAgent(String agentIdentifier, String token) {
    log.info("des clients pour l'agent: {}", agentIdentifier);
    
    List<Client> clients;
    
    //  c'est un email, chercher directement
    if (agentIdentifier != null && agentIdentifier.contains("@")) {
        clients = clientRepository.findByCreatedBy(agentIdentifier);
    } 
    //  c'est un UUID, essayer de trouver l'email via Auth Service
    else {
        String agentEmail = getAgentEmailById(agentIdentifier, token);
        if (agentEmail != null) {
            clients = clientRepository.findByCreatedBy(agentEmail);
        } else {
            clients = clientRepository.findByCreatedBy(agentIdentifier);
        }
    }
    
    if (clients.isEmpty()) {
        log.info("client trouvé pour l'agent: {}", agentIdentifier);
        return List.of();
    }
    
    log.info("clients trouvés pour l'agent: {}", clients.size(), agentIdentifier);
    
    return clients.stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
}
    @Transactional(readOnly = true)
    public List<ClientResponse> getClientsByAgent(String agentId) {
        log.info("des clients pour l'agent: {}", agentId);
    
        //   par email ou par ID
        List<Client> clients = clientRepository.findByCreatedBy(agentId);
    
       //  aucun client trouvé avec l'ID, essayer de chercher par email
       if (clients.isEmpty() && agentId.contains("@")) {
           log.info("client avec created_by={}, tentative avec email", agentId);
           clients = clientRepository.findByCreatedBy(agentId);
       }
    
       if (clients.isEmpty()) {
          log.info("client trouvé pour l'agent: {}", agentId);
          return List.of();
       }
    
         log.info("clients trouvés pour l'agent: {}", clients.size(), agentId);
    
        return clients.stream()
          .map(this::mapToResponse)
          .collect(Collectors.toList());
    }
    
    /**
     * Compte le nombre de clients associés à un agent
     * @param agentId L'identifiant de l'agent
     * @return Nombre de clients
     */
    @Transactional(readOnly = true)
    public long countClientsByAgent(String agentId) {
        return clientRepository.countByCreatedBy(agentId);
    }
    
    
    // 
    //  PRIVÉES
    // 
    
    private ClientResponse mapToResponse(Client client) {
        return ClientResponse.builder()
                .id(client.getId())
                .email(client.getEmail())
                .phoneNumber(client.getPhoneNumber())
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .address(client.getAddress())
                .birthDate(client.getBirthDate())
                .clientType(client.getClientType())
                .status(client.getStatus())
                .creditScore(client.getCreditScore())
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .lastLoginAt(client.getLastLoginAt())
                .agencyId(client.getAgencyId())
                .createdBy(client.getCreatedBy())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> getClientsByAgency(String agencyId) {
        return clientRepository.findByAgencyId(agencyId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClientResponse assignClientToAgent(String clientId, String agentEmail, String agencyId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client non trouvé: " + clientId));
        client.setCreatedBy(agentEmail);
        client.setAgencyId(agencyId);
        client = clientRepository.save(client);
        log.info("Client {} assigné à l'agent {} (agence {})", clientId, agentEmail, agencyId);
        return mapToResponse(client);
    }
 
    
}
