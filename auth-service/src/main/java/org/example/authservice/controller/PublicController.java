package org.example.authservice.controller;

import org.example.authservice.dto.response.UserDTO;
import org.example.authservice.model.Role;
import org.example.authservice.model.User;
import org.example.authservice.model.enums.UserRoleType;
import org.example.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final UserRepository userRepository;
    private final JavaMailSender  mailSender;

    @Value("${spring.mail.host:NON_CONFIGURÉ}")
    private String mailHost;

    @Value("${spring.mail.port:0}")
    private String mailPort;

    @Value("${spring.mail.username:VIDE}")
    private String mailUsername;

    @Value("${spring.mail.password:VIDE}")
    private String mailPassword;

    @Value("${app.mail.from:aa61af001@smtp-brevo.com}")
    private String mailFrom;

    /** Diagnostic SMTP — appeler depuis le navigateur : GET /api/public/test-smtp?to=ton@email.com */
    @GetMapping("/test-smtp")
    public ResponseEntity<Map<String, Object>> testSmtp(@RequestParam String to) {
        Map<String, Object> result = new HashMap<>();
        result.put("host",     mailHost);
        result.put("port",     mailPort);
        result.put("username", mailUsername);
        result.put("password_set", mailPassword != null && !mailPassword.isBlank()
                                   && !mailPassword.equals("VIDE"));
        result.put("to", to);
        result.put("from", mailFrom);
        try {
            var msg = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject("Test SMTP MicroFinanceHub");
            helper.setText("Si vous recevez ce mail, le SMTP fonctionne.", false);
            mailSender.send(msg);
            result.put("status", "SUCCESS");
            result.put("message", "Email envoyé avec succès !");
        } catch (Exception e) {
            result.put("status", "ERREUR");
            result.put("message", e.getMessage());
            result.put("cause",   e.getCause() != null ? e.getCause().getMessage() : null);
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * Vérifier si un utilisateur existe par ID
     * GET /api/public/users/{id}/exists
     */
    @GetMapping("/users/{id}/exists")
    public ResponseEntity<Map<String, Object>> userExists(@PathVariable String id) {
        log.info("Vérification existence utilisateur: {}", id);
        
        return userRepository.findById(id)
                .map(user -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("exists", true);
                    response.put("id", user.getId());
                    response.put("email", user.getEmail());
                    response.put("firstName", user.getFirstName());
                    response.put("lastName", user.getLastName());
                    response.put("fullName", user.getFirstName() + " " + user.getLastName());
                    response.put("role", user.getUserRoleType().name());
                    response.put("isActive", user.isEnabled());
                    
                    log.info("trouvé: {}", user.getEmail());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.warn("non trouvé: {}", id);
                    return ResponseEntity.ok(Map.of("exists", false));
                });
    }
    
    /**
     * Récupérer les informations d'un utilisateur par ID
     * GET /api/public/users/{id}
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable String id) {
        log.info("Récupération utilisateur par ID: {}", id);
        
        return userRepository.findById(id)
                .map(this::mapToUserDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Récupérer les informations d'un utilisateur par email
     * GET /api/public/users/by-email?email=xxx
     */
    @GetMapping("/users/by-email")
    public ResponseEntity<UserDTO> getUserByEmail(@RequestParam String email) {
        log.info("Récupération utilisateur par email: {}", email);
        
        return userRepository.findByEmail(email)
                .map(this::mapToUserDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Valider un directeur (existence + non assigné à une agence)
     * GET /api/public/director/validate/{directorId}
     */
    @GetMapping("/director/validate/{directorId}")
    public ResponseEntity<Map<String, Object>> validateDirector(@PathVariable String directorId) {
        log.info("Validation directeur: {}", directorId);
        
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
                        "Directeur déjà assigné à une agence" : 
                        "Directeur disponible pour assignation");
                    
                    log.info("validé: {} - Peut être assigné: {}", user.getEmail(), !hasAgency);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("exists", false);
                    response.put("canBeAssigned", false);
                    response.put("message", "Le directeur n'existe pas dans le système");
                    log.warn("non trouvé: {}", directorId);
                    return ResponseEntity.ok(response);
                });
    }
    
    /**
     * Valider un agent (existence + rôle AGENT)
     * GET /api/public/agent/validate/{agentId}
     */
    @GetMapping("/agent/validate/{agentId}")
    public ResponseEntity<Map<String, Object>> validateAgent(@PathVariable String agentId) {
        log.info("Validation agent: {}", agentId);
        
        return userRepository.findById(agentId)
                .map(user -> {
                    boolean hasAgentRole = user.getUserRoleType() == UserRoleType.AGENT;
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("exists", true);
                    response.put("id", user.getId());
                    response.put("email", user.getEmail());
                    response.put("firstName", user.getFirstName());
                    response.put("lastName", user.getLastName());
                    response.put("fullName", user.getFirstName() + " " + user.getLastName());
                    response.put("currentRole", user.getUserRoleType().name());
                    response.put("hasAgentRole", hasAgentRole);
                    response.put("isActive", user.isEnabled());
                    response.put("canBeAssigned", hasAgentRole);
                    response.put("message", hasAgentRole ? 
                        "Agent valide et disponible" : 
                        "L'utilisateur n'a pas le rôle AGENT");
                    
                    log.info("validé: {} - Rôle AGENT: {}", user.getEmail(), hasAgentRole);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("exists", false);
                    response.put("canBeAssigned", false);
                    response.put("message", "L'agent n'existe pas dans le système");
                    log.warn("non trouvé: {}", agentId);
                    return ResponseEntity.ok(response);
                });
    }
    
    /**
     * Endpoint de test pour vérifier que le contrôleur public fonctionne
     * GET /api/public/test
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        log.info(" [PUBLIC] Test endpoint - Fonctionne!");
        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "message", "Public controller is working",
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
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

      
}