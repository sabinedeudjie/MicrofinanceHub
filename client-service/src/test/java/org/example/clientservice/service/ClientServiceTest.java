package org.example.clientservice.service;

import org.example.clientservice.client.AuthServiceClient;
import org.example.clientservice.dto.request.ClientRequest;
import org.example.clientservice.dto.response.ClientResponse;
import org.example.clientservice.exception.DuplicateEmailException;
import org.example.clientservice.exception.DuplicatePhoneException;
import org.example.clientservice.model.Client;
import org.example.clientservice.model.enums.ClientStatus;
import org.example.clientservice.model.enums.ClientType;
import org.example.clientservice.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientService — Tests unitaires")
class ClientServiceTest {

    @Mock private ClientRepository    clientRepository;
    @Mock private CreditScoreService  creditScoreService;
    @Mock private AuthServiceClient   authServiceClient;

    @InjectMocks private ClientService clientService;

    private ClientRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new ClientRequest();
        validRequest.setEmail("marie.kouam@mfh.cm");
        validRequest.setFirstName("Marie");
        validRequest.setLastName("Kouam");
        validRequest.setPhoneNumber("237690000001");
        validRequest.setClientType(ClientType.INDIVIDUAL);
    }

    @Test
    @DisplayName("Crée un client avec succès quand l'email et le téléphone sont uniques")
    void shouldCreateClientSuccessfully() {
        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.existsByPhoneNumber(anyString())).thenReturn(false);

        Client saved = Client.builder()
            .id("uuid-client-001")
            .email("marie.kouam@mfh.cm")
            .firstName("Marie")
            .lastName("Kouam")
            .phoneNumber("237690000001")
            .status(ClientStatus.ACTIVE)
            .creditScore(50)
            .createdAt(LocalDateTime.now())
            .build();
        when(clientRepository.save(any(Client.class))).thenReturn(saved);

        ClientResponse response = clientService.createClient(validRequest, "agent-001");

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("marie.kouam@mfh.cm");
        assertThat(response.getFirstName()).isEqualTo("Marie");
        assertThat(response.getStatus()).isEqualTo(ClientStatus.ACTIVE);
        verify(clientRepository, times(1)).save(any(Client.class));
    }

    @Test
    @DisplayName("Rejette la création si l'email existe déjà")
    void shouldThrowWhenEmailAlreadyExists() {
        when(clientRepository.existsByEmail("marie.kouam@mfh.cm")).thenReturn(true);

        assertThatThrownBy(() -> clientService.createClient(validRequest, "agent-001"))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessageContaining("marie.kouam@mfh.cm");

        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rejette la création si le téléphone existe déjà")
    void shouldThrowWhenPhoneAlreadyExists() {
        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.existsByPhoneNumber("237690000001")).thenReturn(true);

        assertThatThrownBy(() -> clientService.createClient(validRequest, "agent-001"))
            .isInstanceOf(DuplicatePhoneException.class);

        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("N'appelle pas existsByPhoneNumber si phoneNumber est null")
    void shouldSkipPhoneCheckWhenPhoneIsNull() {
        validRequest.setPhoneNumber(null);
        when(clientRepository.existsByEmail(anyString())).thenReturn(false);

        Client saved = Client.builder()
            .id("uuid-client-002")
            .email("marie.kouam@mfh.cm")
            .firstName("Marie")
            .lastName("Kouam")
            .status(ClientStatus.ACTIVE)
            .creditScore(50)
            .createdAt(LocalDateTime.now())
            .build();
        when(clientRepository.save(any(Client.class))).thenReturn(saved);

        clientService.createClient(validRequest, "agent-001");

        verify(clientRepository, never()).existsByPhoneNumber(anyString());
    }
}
