package org.example.authservice.service;

import org.example.authservice.model.User;
import org.example.authservice.model.enums.UserRoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService — Tests unitaires")
class JwtServiceTest {

    private JwtService jwtService;
    private User agentUser;
    private User adminUser;

    private static final String TEST_SECRET =
        "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey",      TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration",  86400000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L);

        agentUser = User.builder()
            .id("agent-uuid-001")
            .email("agent@mfh.cm")
            .password("encoded-pwd")
            .firstName("Jean")
            .lastName("Mbarga")
            .userRoleType(UserRoleType.AGENT)
            .enabled(true)
            .build();

        adminUser = User.builder()
            .id("admin-uuid-001")
            .email("admin@mfh.cm")
            .password("encoded-pwd")
            .firstName("Super")
            .lastName("Admin")
            .userRoleType(UserRoleType.ADMIN)
            .enabled(true)
            .build();
    }

    @Test
    @DisplayName("Génère un token JWT non nul et non vide")
    void shouldGenerateNonNullToken() {
        String token = jwtService.generateToken(agentUser);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("Extrait correctement le nom d'utilisateur (email) du token")
    void shouldExtractCorrectUsername() {
        String token = jwtService.generateToken(agentUser);
        assertThat(jwtService.extractUsername(token)).isEqualTo("agent@mfh.cm");
    }

    @Test
    @DisplayName("Extrait le rôle AGENT du token")
    void shouldExtractAgentRole() {
        String token = jwtService.generateToken(agentUser);
        assertThat(jwtService.extractRole(token)).isEqualTo("AGENT");
    }

    @Test
    @DisplayName("Extrait le rôle ADMIN du token")
    void shouldExtractAdminRole() {
        String token = jwtService.generateToken(adminUser);
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Valide le token pour l'utilisateur qui l'a généré")
    void shouldValidateTokenForOwner() {
        String token = jwtService.generateToken(agentUser);
        assertThat(jwtService.isTokenValid(token, agentUser)).isTrue();
    }

    @Test
    @DisplayName("Rejette le token pour un utilisateur différent")
    void shouldRejectTokenForDifferentUser() {
        String token = jwtService.generateToken(agentUser);
        assertThat(jwtService.isTokenValid(token, adminUser)).isFalse();
    }

    @Test
    @DisplayName("Valide un token valide avec la méthode sans UserDetails")
    void shouldValidateTokenWithoutUserDetails() {
        String token = jwtService.generateToken(agentUser);
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("Rejette un token malformé")
    void shouldRejectMalformedToken() {
        assertThat(jwtService.isTokenValid("token.invalide.xyz")).isFalse();
    }

    @Test
    @DisplayName("Génère un token de rafraîchissement différent du token d'accès")
    void shouldGenerateDifferentRefreshToken() {
        String access  = jwtService.generateToken(agentUser);
        String refresh = jwtService.generateRefreshToken(agentUser);
        assertThat(access).isNotEqualTo(refresh);
        assertThat(jwtService.extractUsername(refresh)).isEqualTo("agent@mfh.cm");
    }

    @Test
    @DisplayName("Retourne la durée d'expiration configurée")
    void shouldReturnConfiguredExpiration() {
        assertThat(jwtService.getJwtExpiration()).isEqualTo(86400000L);
    }
}
