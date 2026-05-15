package org.example.authservice.service;

import org.example.authservice.model.PasswordResetToken;
import org.example.authservice.model.User;
import org.example.authservice.repository.PasswordResetTokenRepository;
import org.example.authservice.repository.RefreshTokenRepository;
import org.example.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {
    
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    @Transactional
    public void createPasswordResetToken(String email) {
        log.info("Creation d'un token de reinitialisation pour: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Aucun compte trouve avec l'email: {}", email);
                    return new RuntimeException("Aucun compte associé à cet email");
                });

        try {
            log.info("Suppression de l'ancien token pour l'utilisateur: {}", user.getId());
            tokenRepository.deleteByUserId(user.getId());
            tokenRepository.flush();

            String token = UUID.randomUUID().toString();
            log.info("Nouveau token genere pour: {}", email);
            
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .build();
            
            tokenRepository.save(resetToken);
            tokenRepository.flush();
            
            emailService.sendPasswordResetEmail(user.getEmail(), token);
            
            log.info("Token de reinitialisation cree avec succes pour: {}", email);

        } catch (DataIntegrityViolationException e) {
            log.error("Erreur de contrainte d'integrite: {}", e.getMessage());
            log.info("Tentative de recuperation apres erreur...");
            tokenRepository.deleteByUserId(user.getId());
            tokenRepository.flush();

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .build();
            tokenRepository.save(resetToken);
            tokenRepository.flush();

            emailService.sendPasswordResetEmail(user.getEmail(), token);
            log.info("Token cree apres recuperation pour: {}", email);
        } catch (Exception e) {
            log.error("lors de la création du token: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible de créer le token de réinitialisation");
        }
    }
    
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Reinitialisation du mot de passe avec token: {}", token);

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Token invalide: {}", token);
                    return new RuntimeException("Token invalide");
                });

        if (resetToken.isUsed()) {
            log.warn("Token deja utilise: {}", token);
            throw new RuntimeException("Token déjà utilisé");
        }
        
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("Token expire: {}", token);
            throw new RuntimeException("Token expiré");
        }

        User user = resetToken.getUser();
        log.info("Reinitialisation du mot de passe pour l'utilisateur: {}", user.getEmail());

        // Vérifier que le nouveau mot de passe est différent de l'ancien
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            log.warn("nouveau mot de passe est identique à l'ancien");
            throw new RuntimeException("Le nouveau mot de passe doit être différent de l'ancien");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        refreshTokenRepository.deleteByUserId(user.getId());

        log.info("Mot de passe reinitialise avec succes pour: {}", user.getEmail());
    }
}