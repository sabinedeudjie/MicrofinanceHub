package org.example.authservice.controller;

import org.example.authservice.model.User;
import org.example.authservice.model.enums.UserRoleType;
import org.example.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {
    
    private final UserRepository userRepository;
    
    /**
     * Met à jour le rôle d'un utilisateur (endpoint interne)
     */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Void> updateUserRole(
            @PathVariable String userId,
            @RequestParam String role) {
        
        log.info("   [INTERNAL] Mise à jour du rôle pour l'utilisateur: {}", userId);
        log.info("   Nouveau rôle: {}", role);
        
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isEmpty()) {
            log.warn("   Utilisateur non trouvé: {}", userId);
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        
        try {
            UserRoleType newRole = UserRoleType.valueOf(role.toUpperCase());
            user.setUserRoleType(newRole);
            userRepository.save(user);
            log.info("   Rôle mis à jour pour: {} -> {}", user.getEmail(), role);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("   Rôle invalide: {}", role);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Met à jour l'agence d'un utilisateur (endpoint interne)
     */
    @PutMapping("/users/{userId}/agency")
    public ResponseEntity<Void> updateUserAgency(
            @PathVariable String userId,
            @RequestParam(required = false) String agencyId,
            @RequestParam(required = false) String agencyCode) {
        
        log.info("   [INTERNAL] Mise à jour de l'agence pour l'utilisateur: {}", userId);
        log.info("   Agency ID: {}", agencyId);
        log.info("   Agency Code: {}", agencyCode);
        
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isEmpty()) {
            log.warn("   Utilisateur non trouvé: {}", userId);
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        user.setAgencyId(agencyId);
        user.setAgencyCode(agencyCode);
        userRepository.save(user);
        
        log.info("   Agence mise à jour pour: {}", user.getEmail());
        return ResponseEntity.ok().build();
    }
    
    /**
     * Met à jour l'agence et l'assignedBy d'un utilisateur (endpoint interne)
     */
  
@PutMapping("/users/{userId}/agency/with-assigner")
public ResponseEntity<Void> updateUserAgencyWithAssigner(
        @PathVariable String userId,
        @RequestParam(required = false) String agencyId,
        @RequestParam(required = false) String agencyCode,
        @RequestParam(required = false) String assignedBy,
        @RequestParam(required = false) String assignedByName) {
    
    log.info("[INTERNAL] Mise à jour de l'agence avec assigneur: {}", userId);
    log.info("   Agency ID: {}", agencyId);
    log.info("   Agency Code: {}", agencyCode);
    log.info("   Assigned By: {}", assignedBy);
    log.info("   Assigned By Name: {}", assignedByName);
    
    Optional<User> userOpt = userRepository.findById(userId);
    
    if (userOpt.isEmpty()) {
        log.warn("   Utilisateur non trouvé: {}", userId);
        return ResponseEntity.notFound().build();
    }
    
    User user = userOpt.get();
    
    if (agencyId != null) {
        user.setAgencyId(agencyId);
    }
    if (agencyCode != null) {
        user.setAgencyCode(agencyCode);
    }
    if (assignedBy != null) {
        user.setAssignedBy(assignedBy);
    }
    //  n'existe pas dans l'entité User, donc on utilise assignedBy
    user.setAssignedAt(LocalDateTime.now());
    userRepository.save(user);
    
    log.info("Agence mise a jour pour: {} -> Agency: {} ({}), Assigne par: {}",
        user.getEmail(), agencyCode, agencyId, assignedBy);
    return ResponseEntity.ok().build();
}
}