package org.example.accountservice.service;

import org.example.accountservice.dto.CompteResponse;
import org.example.accountservice.dto.OuvrirCompteRequest;
import org.example.accountservice.exception.CompteNotFoundException;
import org.example.accountservice.model.*;
import org.example.accountservice.repository.CompteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompteService — Tests unitaires")
class CompteServiceTest {

    @Mock private CompteRepository compteRepository;
    @Mock private RabbitTemplate   rabbitTemplate;

    @InjectMocks private CompteService compteService;

    private Compte compteActif;

    @BeforeEach
    void setUp() {
        compteActif = Compte.builder()
            .id(1L)
            .clientId("client-uuid-001")
            .numeroCompte("MFH-20250001")
            .typeCompte(TypeCompte.EPARGNE)
            .solde(new BigDecimal("150000.00"))
            .devise(Devise.XAF)
            .statut(StatutCompte.ACTIF)
            .dateOuverture(LocalDateTime.now())
            .tauxInteret(new BigDecimal("0.0350"))
            .soldeMinimum(BigDecimal.ZERO)
            .build();
    }

    @Test
    @DisplayName("Ouvre un compte épargne avec succès")
    void shouldOpenEpargneAccount() {
        OuvrirCompteRequest request = OuvrirCompteRequest.builder()
            .clientId("client-uuid-001")
            .typeCompte(TypeCompte.EPARGNE)
            .soldeInitial(new BigDecimal("50000"))
            .build();

        Compte saved = Compte.builder()
            .id(2L)
            .clientId("client-uuid-001")
            .numeroCompte("MFH-20250002")
            .typeCompte(TypeCompte.EPARGNE)
            .solde(new BigDecimal("50000"))
            .devise(Devise.XAF)
            .statut(StatutCompte.EN_ATTENTE_VALIDATION)
            .dateOuverture(LocalDateTime.now())
            .tauxInteret(new BigDecimal("0.0350"))
            .soldeMinimum(BigDecimal.ZERO)
            .build();

        when(compteRepository.save(any(Compte.class))).thenReturn(saved);

        CompteResponse response = compteService.ouvrirCompte(request);

        assertThat(response).isNotNull();
        assertThat(response.getClientId()).isEqualTo("client-uuid-001");
        assertThat(response.getTypeCompte()).isEqualTo(TypeCompte.EPARGNE);
        assertThat(response.getSolde()).isEqualByComparingTo(new BigDecimal("50000"));
        verify(compteRepository, times(1)).save(any(Compte.class));
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), (Object) any());
    }

    @Test
    @DisplayName("Ouvre un compte courant avec solde initial nul")
    void shouldOpenCourantAccountWithZeroBalance() {
        OuvrirCompteRequest request = OuvrirCompteRequest.builder()
            .clientId("client-uuid-002")
            .typeCompte(TypeCompte.COURANT)
            .build();

        Compte saved = Compte.builder()
            .id(3L)
            .clientId("client-uuid-002")
            .numeroCompte("MFH-20250003")
            .typeCompte(TypeCompte.COURANT)
            .solde(BigDecimal.ZERO)
            .devise(Devise.XAF)
            .statut(StatutCompte.EN_ATTENTE_VALIDATION)
            .dateOuverture(LocalDateTime.now())
            .tauxInteret(new BigDecimal("0.0100"))
            .soldeMinimum(BigDecimal.ZERO)
            .build();

        when(compteRepository.save(any(Compte.class))).thenReturn(saved);

        CompteResponse response = compteService.ouvrirCompte(request);

        assertThat(response.getSolde()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Retourne le solde d'un compte existant")
    void shouldReturnBalanceForExistingAccount() {
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteActif));

        BigDecimal solde = compteService.consulterSolde(1L);

        assertThat(solde).isEqualByComparingTo(new BigDecimal("150000.00"));
    }

    @Test
    @DisplayName("Lève une exception pour un compte inexistant")
    void shouldThrowWhenAccountNotFound() {
        when(compteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compteService.consulterSolde(999L))
            .isInstanceOf(CompteNotFoundException.class);
    }

    @Test
    @DisplayName("Récupère un compte par son ID")
    void shouldGetAccountById() {
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteActif));

        CompteResponse response = compteService.getCompteById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getNumeroCompte()).isEqualTo("MFH-20250001");
        assertThat(response.getSolde()).isEqualByComparingTo(new BigDecimal("150000.00"));
    }
}
