package org.example.authservice.controller;

import org.example.authservice.client.AgencyServiceClient;
import org.example.authservice.dto.request.*;
import org.example.authservice.dto.response.AgentAssignmentResponse;
import org.example.authservice.dto.response.LoginResponse;
import org.example.authservice.dto.response.UserDTO;
import org.example.authservice.dto.response.UserResponse;
import org.example.authservice.model.RefreshToken;
import org.example.authservice.model.Role;
import org.example.authservice.model.User;
import org.example.authservice.model.UserSession;
import org.example.authservice.model.enums.UserRoleType;
import org.example.authservice.repository.RefreshTokenRepository;
import org.example.authservice.repository.UserRepository;
import org.example.authservice.repository.UserSessionRepository;
import org.example.authservice.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final SecurityQuestionService securityQuestionService;
    private final SessionService sessionService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSessionRepository userSessionRepository;
    private final AgencyServiceClient agencyServiceClient;
    
    
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, 
                                                HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getCurrentUser(userDetails.getUsername()));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        try {
            authService.changePassword(userDetails.getUsername(), request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createAdmin(
            @Valid @RequestBody RegisterRequest request,
            @AuthenticationPrincipal UserDetails caller) {
        if (!isSuperAdmin(caller)) {
            return ResponseEntity.status(403).body(Map.of("message", "Seul le super administrateur peut créer d'autres administrateurs."));
        }
        return ResponseEntity.ok(authService.registerAdmin(request));
    }

    @PostMapping("/agent/create")
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTEUR_AGENCE')")
    public ResponseEntity<LoginResponse> createAgent(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registerAgent(request));
    }

    @PostMapping("/admin/create-directeur")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoginResponse> createDirecteur(@Valid @RequestBody RegisterRequest request) {
        log.info("d'un directeur d'agence par l'admin");
        return ResponseEntity.ok(authService.registerDirecteur(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            passwordResetService.createPasswordResetToken(request.getEmail());
            return ResponseEntity.ok(Map.of(
                "message", "Si un compte existe avec cet email, un lien de réinitialisation vous a été envoyé"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(Map.of(
                "message", "Si un compte existe avec cet email, un lien de réinitialisation vous a été envoyé"
            ));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 
    //  DE DÉCONNEXION
    // 
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenBlacklistService.blacklistToken(token);
            try {
                String username = jwtService.extractUsername(token);
                log.info("déconnecté: {}", username);
            } catch (Exception e) {
                log.info("invalide lors de la déconnexion");
            }
        }
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }
    
    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logoutAll(@AuthenticationPrincipal UserDetails userDetails) {
        userRepository.findByEmail(userDetails.getUsername())
                .ifPresent(user -> {
                    tokenBlacklistService.blacklistAllUserTokens(user.getId());
                    log.info("de tous les appareils pour: {}", user.getEmail());
                });
        return ResponseEntity.ok(Map.of("message", "Déconnexion de tous les appareils réussie"));
    }
    
    // 
    //  POUR LES QUESTIONS DE SÉCURITÉ
    // 
    
    @GetMapping("/security-questions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSecurityQuestions() {
        Map<String, String> questions = securityQuestionService.getQuestionsWithKeys();
        return ResponseEntity.ok(Map.of("questions", questions));
    }
    
    @PostMapping("/security-question/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> setupSecurityQuestion(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SecurityQuestionRequest request) {
        securityQuestionService.setSecurityQuestion(
            userDetails.getUsername(),
            request.getQuestion(),
            request.getAnswer()
        );
        return ResponseEntity.ok(Map.of("message", "Question de sécurité configurée avec succès"));
    }
    
    @GetMapping("/security-question/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT','DIRECTEUR_AGENCE')") 
    public ResponseEntity<?> checkSecurityQuestionStatus(@RequestParam String email) {
        boolean hasQuestion = securityQuestionService.hasSecurityQuestion(email);
        return ResponseEntity.ok(Map.of(
            "hasSecurityQuestion", hasQuestion,
            "email", email
        ));
    }
    
    @PostMapping("/reset-password-with-question")
    public ResponseEntity<?> resetPasswordWithQuestion(
            @Valid @RequestBody ResetPasswordWithQuestionRequest request) {
        try {
            securityQuestionService.resetPasswordWithQuestion(
                request.getEmail(),
                request.getSecurityAnswer(),
                request.getNewPassword()
            );
            return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/security-question")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> disableSecurityQuestion(@AuthenticationPrincipal UserDetails userDetails) {
        securityQuestionService.disableSecurityQuestion(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Question de sécurité désactivée"));
    }
    
    // 
    //  DE SESSION
    // 
    
    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getActiveSessions(@AuthenticationPrincipal UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .map(user -> {
                    try {
                        List<UserSession> sessions = sessionService.getActiveSessions(user.getId());
                        List<Map<String, Object>> sessionList = sessions.stream()
                            .map(session -> {
                                Map<String, Object> sessionMap = new HashMap<>();
                                sessionMap.put("id", session.getId());
                                sessionMap.put("deviceInfo", session.getDeviceInfo());
                                sessionMap.put("ipAddress", session.getIpAddress());
                                sessionMap.put("isActive", session.isActive());
                                sessionMap.put("lastActivity", session.getLastActivity() != null ? session.getLastActivity().toString() : null);
                                sessionMap.put("createdAt", session.getCreatedAt() != null ? session.getCreatedAt().toString() : null);
                                return sessionMap;
                            })
                            .collect(Collectors.toList());
                        return ResponseEntity.ok(Map.of("sessions", sessionList, "total", sessionList.size()));
                    } catch (Exception e) {
                        log.error("lors de la récupération des sessions", e);
                        return ResponseEntity.status(500).body(Map.of("error", "Erreur interne du serveur"));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Utilisateur non trouvé")));
    }
    
    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> revokeSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .map(user -> {
                    boolean revoked = sessionService.revokeSession(sessionId, user.getId());
                    if (revoked) {
                        return ResponseEntity.ok(Map.of("message", "Session révoquée avec succès"));
                    } else {
                        return ResponseEntity.badRequest().body(Map.of("error", "Session non trouvée"));
                    }
                })
                .orElse(ResponseEntity.badRequest().body(Map.of("error", "Utilisateur non trouvé")));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        log.info(" Validation de token JWT");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("manquant ou mal formaté");
            return ResponseEntity.status(401).body(Map.of("valid", false, "error", "Token manquant"));
        }
        
        String token = authHeader.substring(7);
        
        try {
            boolean isValid = jwtService.isTokenValid(token);
            if (isValid) {
                String username = jwtService.extractUsername(token);
                log.info("valide pour: {}", username);
                return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "username", username,
                    "role", jwtService.extractRole(token)
                ));
            } else {
                log.warn("invalide");
                return ResponseEntity.status(401).body(Map.of("valid", false, "error", "Token invalide"));
            }
        } catch (Exception e) {
            log.error("lors de la validation: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("valid", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        if (refreshToken != null && refreshToken.startsWith("Bearer ")) {
            String token = refreshToken.substring(7);
            try {
                String username = jwtService.extractUsername(token);
                User user = userRepository.findByEmail(username)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                
                RefreshToken savedToken = refreshTokenRepository.findByToken(token)
                        .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
                
                if (savedToken.isRevoked() || savedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                    throw new RuntimeException("Refresh token expired or revoked");
                }
                
                String newAccessToken = jwtService.generateToken(user);
                
                return ResponseEntity.ok(Map.of(
                    "access_token", newAccessToken,
                    "token_type", "Bearer",
                    "expires_in", jwtService.getJwtExpiration()
                ));
            } catch (Exception e) {
                return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
            }
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Refresh token required"));
    }
    
    // 
    //  POUR LA GESTION DES UTILISATEURS
    // 
    
    @GetMapping("/users/by-role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<List<UserDTO>> getUsersByRole(@PathVariable String role) {
        log.info("des utilisateurs avec le rôle: {}", role);
        try {
            org.example.authservice.model.enums.UserRoleType roleType =
                org.example.authservice.model.enums.UserRoleType.valueOf(role.toUpperCase());
            List<UserDTO> users = userRepository.findByUserRoleType(roleType).stream()
                .map(this::mapToUserDTO)
                .collect(Collectors.toList());
            return ResponseEntity.ok(users);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable String id) {
        log.info("de l'utilisateur par ID: {}", id);
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(mapToUserDTO(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users/by-email")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<UserDTO> getUserByEmail(@RequestParam String email) {
        log.info("de l'utilisateur par email: {}", email);
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(mapToUserDTO(user)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/users/{userId}/exists")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<Boolean> userExists(@PathVariable String userId) {
        boolean exists = userRepository.existsById(userId);
        return ResponseEntity.ok(exists);
    }

    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateUserRole(@PathVariable String userId, @RequestParam String role) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setUserRoleType(UserRoleType.valueOf(role));
            userRepository.save(user);
        });
        return ResponseEntity.ok().build();
    }
    
    // 
    //  INTERNES POUR LES SERVICES
    // 
    
    @PutMapping("/api/internal/users/{userId}/agency")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<Void> updateUserAgency(
            @PathVariable String userId,
            @RequestParam(required = false) String agencyId,
            @RequestParam(required = false) String agencyCode) {
        
        log.info("à jour de l'agence pour l'utilisateur: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        user.setAgencyId(agencyId);
        user.setAgencyCode(agencyCode);
        userRepository.save(user);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Endpoint pour vérifier si un agent existe (utilisé par Account Service)
     */
    @GetMapping("/api/internal/agent/exists/{agentId}")
    public ResponseEntity<Map<String, Object>> checkAgentExists(@PathVariable String agentId) {
        log.info("de l'existence de l'agent: {}", agentId);
        
        return userRepository.findById(agentId)
                .map(user -> {
                    boolean hasAgentRole = user.getRoles().stream()
                            .anyMatch(role -> "AGENT".equals(role.getName()) || "ROLE_AGENT".equals(role.getName()));
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("exists", true);
                    response.put("hasAgentRole", hasAgentRole);
                    response.put("email", user.getEmail());
                    response.put("firstName", user.getFirstName());
                    response.put("lastName", user.getLastName());
                    response.put("userRoleType", user.getUserRoleType().name());
                    
                    log.info("trouvé: {}, a le rôle AGENT: {}", user.getEmail(), hasAgentRole);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.warn("non trouvé avec ID: {}", agentId);
                    return ResponseEntity.ok(Map.of("exists", false));
                });
    }
    
    /**
     * Endpoint PUBLIC pour vérifier si un agent existe (sans authentification)
     */
    @GetMapping("/public/agent/exists/{agentId}")
    public ResponseEntity<Map<String, Object>> publicAgentExists(@PathVariable String agentId) {
        log.info(" [PUBLIC] Vérification de l'agent: {}", agentId);
        
        return userRepository.findById(agentId)
                .map(user -> {
                    boolean hasAgentRole = user.getUserRoleType() == UserRoleType.AGENT;
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("exists", true);
                    response.put("hasAgentRole", hasAgentRole);
                    response.put("email", user.getEmail());
                    response.put("firstName", user.getFirstName());
                    response.put("lastName", user.getLastName());
                    response.put("userRoleType", user.getUserRoleType().name());
                    
                    log.info("trouvé: {}, rôle AGENT: {}", user.getEmail(), hasAgentRole);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.warn("non trouvé: {}", agentId);
                    return ResponseEntity.ok(Map.of("exists", false));
                });
    }
    
    /**
     * Endpoint public pour vérifier si un directeur existe et peut être assigné
     * Utilisé par Agency Service avant de créer/assigner un directeur
     */
    @GetMapping("/public/director/validate/{directorId}")
    public ResponseEntity<Map<String, Object>> validateDirector(@PathVariable String directorId) {
        log.info(" [PUBLIC] Validation du directeur: {}", directorId);
        
        return userRepository.findById(directorId)
                .map(user -> {
                    boolean hasDirectorRole = user.getUserRoleType() == UserRoleType.DIRECTEUR_AGENCE;
                    boolean hasAgency = user.getAgencyId() != null && !user.getAgencyId().isEmpty();
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("exists", true);
                    response.put("id", user.getId());
                    response.put("email", user.getEmail());
                    response.put("firstName", user.getFirstName());
                    response.put("lastName", user.getLastName());
                    response.put("fullName", user.getFirstName() + " " + user.getLastName());
                    response.put("currentRole", user.getUserRoleType().name());
                    response.put("hasDirectorRole", hasDirectorRole);
                    response.put("hasAgency", hasAgency);
                    response.put("currentAgencyId", user.getAgencyId());
                    response.put("currentAgencyCode", user.getAgencyCode());
                    response.put("canBeAssigned", !hasAgency);
                    response.put("message", hasAgency ? 
                        "Directeur déjà assigné à une agence (ID: " + user.getAgencyId() + ")" : 
                        "Directeur disponible pour assignation");
                    
                    log.info("trouvé: {}, peut être assigné: {}", user.getEmail(), !hasAgency);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.warn("non trouvé: {}", directorId);
                    Map<String, Object> response = new HashMap<>();
                    response.put("exists", false);
                    response.put("canBeAssigned", false);
                    response.put("message", "Le directeur n'existe pas dans le système");
                    return ResponseEntity.ok(response);
                });
    }
    
    /**
     * Endpoint public pour vérifier si un utilisateur existe et son rôle
     * Utilisé par Agency Service
     */
    @GetMapping("/public/user/validate/{userId}")
    public ResponseEntity<Map<String, Object>> validateUser(@PathVariable String userId) {
        log.info(" [PUBLIC] Validation de l'utilisateur: {}", userId);
        
        return userRepository.findById(userId)
                .map(user -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("exists", true);
                    response.put("id", user.getId());
                    response.put("email", user.getEmail());
                    response.put("firstName", user.getFirstName());
                    response.put("lastName", user.getLastName());
                    response.put("fullName", user.getFirstName() + " " + user.getLastName());
                    response.put("currentRole", user.getUserRoleType().name());
                    response.put("isActive", user.isEnabled());
                    
                    log.info("trouvé: {}, rôle: {}", user.getEmail(), user.getUserRoleType().name());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.warn("non trouvé: {}", userId);
                    return ResponseEntity.ok(Map.of("exists", false));
                });
    }

    @GetMapping("/public/test")
    public ResponseEntity<String> testPublic() {
        log.info(" TEST PUBLIC - Fonctionne!");
        return ResponseEntity.ok("Public endpoint works!");
    }


/**
 * Récupérer un utilisateur par son ID (sans authentification pour les services internes)
 */
@GetMapping("/public/users/{id}")
public ResponseEntity<UserDTO> getPublicUserById(@PathVariable String id) {
    log.info("Récupération de l'utilisateur par ID: {}", id);
    
    return userRepository.findById(id)
            .map(user -> ResponseEntity.ok(mapToUserDTO(user)))
            .orElse(ResponseEntity.notFound().build());
}

/**
 * Récupérer un utilisateur par son email (sans authentification pour les services internes)
 */
@GetMapping("/public/users/by-email")
public ResponseEntity<UserDTO> getPublicUserByEmail(@RequestParam String email) {
    log.info("Récupération de l'utilisateur par email: {}", email);
    
    return userRepository.findByEmail(email)
            .map(user -> ResponseEntity.ok(mapToUserDTO(user)))
            .orElse(ResponseEntity.notFound().build());
}
    
    // ── Gestion CRUD des utilisateurs (ADMIN) ──────────────────────────────────

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal UserDetails caller) {
        log.info("Mise à jour de l'utilisateur: {}", userId);
        return userRepository.findById(userId)
                .map(user -> {
                    if (user.getUserRoleType() == UserRoleType.ADMIN && !isSuperAdmin(caller)) {
                        return ResponseEntity.status(403)
                                .body(Map.of("message", "Seul le super administrateur peut modifier un autre administrateur."));
                    }
                    user.setFirstName(request.getFirstName());
                    user.setLastName(request.getLastName());
                    if (request.getPhoneNumber() != null) {
                        user.setPhoneNumber(request.getPhoneNumber());
                    }
                    userRepository.save(user);
                    log.info("Utilisateur {} mis à jour avec succès", userId);
                    return ResponseEntity.ok(mapToUserDTO(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetails caller) {
        log.info("Suppression de l'utilisateur: {}", userId);
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        // Seul le super admin peut supprimer un autre admin
        userRepository.findById(userId).ifPresent(target -> {
            if (target.getUserRoleType() == UserRoleType.ADMIN && !isSuperAdmin(caller)) {
                throw new RuntimeException("Seul le super administrateur peut supprimer un administrateur.");
            }
        });
        // Nettoyer les assignations agence avant suppression
        userRepository.findById(userId).ifPresent(target -> {
            try {
                if (target.getUserRoleType() == UserRoleType.AGENT) {
                    agencyServiceClient.removeAgentOnDeletion(userId);
                } else if (target.getUserRoleType() == UserRoleType.DIRECTEUR_AGENCE) {
                    agencyServiceClient.removeDirectorOnDeletion(userId);
                }
            } catch (Exception e) {
                log.warn("Impossible de nettoyer l'assignation agence pour {}: {}", userId, e.getMessage());
            }
        });
        userSessionRepository.deleteByUserId(userId);
        refreshTokenRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
        log.info("Utilisateur {} supprimé avec succès", userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{userId}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> toggleUserEnabled(@PathVariable String userId) {
        log.info("Toggle actif/inactif de l'utilisateur: {}", userId);
        return userRepository.findById(userId)
                .map(user -> {
                    user.setEnabled(!user.isEnabled());
                    userRepository.save(user);
                    log.info("Utilisateur {} {} avec succès", userId, user.isEnabled() ? "activé" : "désactivé");
                    return ResponseEntity.ok(mapToUserDTO(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private UserDTO mapToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .userRoleType(user.getUserRoleType().name())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .lastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null)
                .agencyId(user.getAgencyId())
                .agencyCode(user.getAgencyCode())
                .build();
    }

/**
 * Met à jour l'agence d'un utilisateur (endpoint interne)
 */
@PutMapping("/internal/users/{userId}/agency")
public ResponseEntity<Void> updateUserAgencyInternal(
        @PathVariable String userId,
        @RequestParam(required = false) String agencyId,
        @RequestParam(required = false) String agencyCode) {
    
    log.info("Mise à jour de l'agence pour l'utilisateur: {}", userId);
    log.info("   Agency ID: {}", agencyId);
    log.info("   Agency Code: {}", agencyCode);
    
    return userRepository.findById(userId)
        .map(user -> {
            user.setAgencyId(agencyId);
            user.setAgencyCode(agencyCode);
            userRepository.save(user);
            log.info("mise à jour pour: {}", user.getEmail());
            return ResponseEntity.ok().<Void>build();
        })
        .orElse(ResponseEntity.notFound().build());
}

/**
 * Met à jour le rôle d'un utilisateur (endpoint interne)
 */
@PutMapping("/internal/users/{userId}/role")
public ResponseEntity<Void> updateUserRoleInternal(
        @PathVariable String userId,
        @RequestParam String role) {
    
    log.info("Mise à jour du rôle pour l'utilisateur: {}", userId);
    log.info("   Nouveau rôle: {}", role);
    log.info("INTERNAL ENDPOINT APPELE");
    log.info("   userId: {}", userId);
    log.info("   role: {}", role);
    log.info("   Path: /internal/users/{}/role", userId);
    
    Optional<User> userOpt = userRepository.findById(userId);
    
    if (userOpt.isEmpty()) {
        log.warn("non trouvé: {}", userId);
        return ResponseEntity.notFound().build();
    }
    
    User user = userOpt.get();
    
    try {
        user.setUserRoleType(UserRoleType.valueOf(role));
        userRepository.save(user);
        log.info("mis à jour pour: {} -> {}", user.getEmail(), role);
        return ResponseEntity.ok().build();
    } catch (IllegalArgumentException e) {
        log.error("invalide: {}", role);
        return ResponseEntity.badRequest().build();
    }
}

    
    // 
    //  DE VÉRIFICATION D'EMAIL (ADMIN)
    // 
    
    /**
     * Vérifie si un email existe dans la table des utilisateurs
     * GET /auth/admin/check-email?email=xxx
     * Accessible uniquement par ADMIN
     */
    @GetMapping("/admin/check-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkEmailExists(@RequestParam String email) {
        log.info("de l'existence de l'email: {} par ADMIN", email);
        
        boolean exists = userRepository.existsByEmail(email);
        
        Map<String, Object> response = new HashMap<>();
        response.put("email", email);
        response.put("exists", exists);
        response.put("message", exists ? "Email déjà utilisé" : "Email disponible");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Vérifie si un email existe (version publique sans authentification)
     * GET /auth/public/check-email?email=xxx
     * Utile pour la validation côté frontend
     */
    @GetMapping("/public/check-email")
    public ResponseEntity<Map<String, Object>> publicCheckEmailExists(@RequestParam String email) {
        log.info("publique de l'existence de l'email: {}", email);
        
        boolean exists = userRepository.existsByEmail(email);
        
        Map<String, Object> response = new HashMap<>();
        response.put("email", email);
        response.put("exists", exists);
        response.put("message", exists ? "Email déjà utilisé" : "Email disponible");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/public/agent/active/{email}")
    public ResponseEntity<Boolean> isAgentActive(@PathVariable String email) {
    log.info("si l'agent est actif: {}", email);
    
    Optional<User> userOpt = userRepository.findByEmail(email);
    if (userOpt.isEmpty()) {
        return ResponseEntity.ok(false);
    }
    
    User user = userOpt.get();
    boolean isActive = user.isEnabled() && user.getUserRoleType() == UserRoleType.AGENT;
    
    //  dans agent_assignments si l'agent a une assignation active
    if (isActive && agencyServiceClient != null) {
        try {
            AgentAssignmentResponse assignment = agencyServiceClient.getAgentAssignmentByEmail(email, null);
            isActive = assignment != null && assignment.isActive();
        } catch (Exception e) {
            log.warn("de vérifier l'assignation: {}", e.getMessage());
        }
    }
    
    return ResponseEntity.ok(isActive);
}

    private static final String SUPER_ADMIN_EMAIL = "admin@mfh.com";

    private boolean isSuperAdmin(UserDetails userDetails) {
        return userDetails != null && SUPER_ADMIN_EMAIL.equals(userDetails.getUsername());
    }
}