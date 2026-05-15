package org.example.authservice.service;

import org.example.authservice.model.User;
import org.example.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityQuestionService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    //  de sécurité prédéfinies
    public static final Map<String, String> DEFAULT_QUESTIONS = Map.of(
        "PETS_NAME", "Quel était le nom de votre premier animal de compagnie ?",
        "BIRTH_CITY", "Dans quelle ville êtes-vous né(e) ?",
        "MOTHER_MAIDEN_NAME", "Quel est le nom de jeune fille de votre mère ?",
        "FAVORITE_TEACHER", "Quel était le nom de votre professeur préféré ?",
        "FIRST_CAR", "Quelle était votre première voiture ?",
        "FAVORITE_MOVIE", "Quel est votre film préféré ?",
        "FAVORITE_BOOK", "Quel est votre livre préféré ?"
    );
    
    public List<String> getAvailableQuestions() {
        return List.copyOf(DEFAULT_QUESTIONS.values());
    }
    
    public Map<String, String> getQuestionsWithKeys() {
        return DEFAULT_QUESTIONS;
    }
    
    @Transactional
    public void setSecurityQuestion(String email, String question, String answer) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        String hashedAnswer = passwordEncoder.encode(answer.toLowerCase().trim());
        
        user.setSecurityQuestion(question);
        user.setSecurityAnswerHash(hashedAnswer);
        user.setSecurityQuestionEnabled(true);
        
        userRepository.save(user);
        
        log.info("de sécurité configurée pour: {}", email);
    }
    
    @Transactional
    public void resetPasswordWithQuestion(String email, String answer, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        if (!user.isSecurityQuestionEnabled()) {
            throw new RuntimeException("Aucune question de sécurité configurée pour ce compte");
        }
        
        if (user.getSecurityAnswerHash() == null) {
            throw new RuntimeException("Question de sécurité non configurée");
        }
        
        //  la réponse (insensible à la casse, trim)
        if (!passwordEncoder.matches(answer.toLowerCase().trim(), user.getSecurityAnswerHash())) {
            throw new RuntimeException("Réponse incorrecte");
        }
        
        //  le mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("de passe réinitialisé via question de sécurité pour: {}", email);
    }
    
    @Transactional
    public void disableSecurityQuestion(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        user.setSecurityQuestionEnabled(false);
        user.setSecurityQuestion(null);
        user.setSecurityAnswerHash(null);
        userRepository.save(user);
        
        log.info("de sécurité désactivée pour: {}", email);
    }
    
    public boolean hasSecurityQuestion(String email) {
        return userRepository.findByEmail(email)
                .map(User::isSecurityQuestionEnabled)
                .orElse(false);
    }
}