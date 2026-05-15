package org.example.authservice.service;

import org.example.authservice.dto.request.LoginRequest;
import org.example.authservice.dto.request.RegisterRequest;
import org.example.authservice.dto.response.LoginResponse;
import org.example.authservice.dto.response.UserResponse;
import org.example.authservice.event.UserLoginEventPublisher;
import org.example.authservice.model.RefreshToken;
import org.example.authservice.model.Role;
import org.example.authservice.model.User;
import org.example.authservice.model.UserSession;
import org.example.authservice.model.enums.UserRoleType;
import org.example.authservice.repository.RefreshTokenRepository;
import org.example.authservice.repository.RoleRepository;
import org.example.authservice.repository.UserRepository;
import org.example.authservice.service.client.ClientServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final SessionService sessionService;
    private final ClientServiceClient clientServiceClient;
    private final UserLoginEventPublisher userLoginEventPublisher;
    
    private static String sanitizePhone(String phone) {
        return (phone == null || phone.isBlank()) ? null : phone.trim();
    }

    private static String nodeText(JsonNode node, String field, String fallback) {
        if (node != null && node.has(field) && !node.get(field).isNull()) {
            String val = node.get(field).asText();
            return val.isBlank() ? fallback : val;
        }
        return fallback;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        log.info("Tentative d'inscription pour: {}", request.getEmail());

        boolean clientExists = clientServiceClient.clientExistsByEmail(request.getEmail());

        if (!clientExists) {
            log.warn("Inscription refusee - Client non trouve: {}", request.getEmail());
            throw new RuntimeException("Inscription impossible : Vous ne faites pas encore partie de nos clients. Veuillez contacter votre agence pour être enregistré.");
        }

        log.info("Client verifie et existant: {}", request.getEmail());

        //  si l'utilisateur existe déjà dans Auth DB
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé. Veuillez vous connecter.");
        }

        // Récupérer les données complètes du client depuis client-service
        JsonNode clientData = clientServiceClient.getClientByEmailPublic(request.getEmail());
        String firstName  = nodeText(clientData, "firstName",  request.getFirstName());
        String lastName   = nodeText(clientData, "lastName",   request.getLastName());
        String phoneNumber = nodeText(clientData, "phoneNumber", request.getPhoneNumber());
        String address    = nodeText(clientData, "address", null);
        LocalDateTime birthDate = null;
        if (clientData != null && clientData.has("birthDate") && !clientData.get("birthDate").isNull()) {
            try { birthDate = LocalDateTime.parse(clientData.get("birthDate").asText()); } catch (Exception ignored) {}
        }

        //  le rôle par défaut
        Role clientRole = roleRepository.findByName("ROLE_CLIENT")
                .orElseThrow(() -> new RuntimeException("Rôle CLIENT non trouvé"));

        //  le nouvel utilisateur
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(sanitizePhone(phoneNumber))
                .address(address)
                .birthDate(birthDate)
                .userRoleType(UserRoleType.CLIENT)
                .enabled(true)
                .build();
        
        user.getRoles().add(clientRole);
        userRepository.save(user);
        
        //  les tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        //  le refresh token
        saveRefreshToken(user, refreshToken);
        
        log.info("Inscription reussie pour: {}", user.getEmail());
        
        String status = nodeText(clientData, "status", "ACTIVE");

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .role(user.getUserRoleType().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .agencyId(user.getAgencyId())
                .expiresIn(jwtService.getJwtExpiration())
                .status(status)
                .build();
    }



    @Transactional
    public LoginResponse registerAdmin(RegisterRequest request) {
        //  si l'utilisateur existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }
        
        //  le rôle ADMIN
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("Rôle ADMIN non trouvé"));
        
        //  le nouvel utilisateur avec le rôle ADMIN
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(sanitizePhone(request.getPhoneNumber()))
                .userRoleType(UserRoleType.ADMIN)
                .enabled(true)
                .build();
        
        user.getRoles().add(adminRole);
        userRepository.save(user);
        
        //  les tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        //  le refresh token
        saveRefreshToken(user, refreshToken);
        
        return LoginResponse.builder()
                .accessToken(accessToken)
                .email(user.getEmail())
                .role(user.getUserRoleType().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .agencyId(user.getAgencyId())
                .expiresIn(jwtService.getJwtExpiration())
                .status("ACTIVE")
                .build();
    }

    @Transactional
    public LoginResponse registerAgent(RegisterRequest request) {
        //  si l'utilisateur existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }
        
        //  le rôle AGENT
        Role agentRole = roleRepository.findByName("ROLE_AGENT")
                .orElseThrow(() -> new RuntimeException("Rôle AGENT non trouvé"));
        
        //  le nouvel utilisateur avec le rôle AGENT
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(sanitizePhone(request.getPhoneNumber()))
                .userRoleType(UserRoleType.AGENT)
                .enabled(true)
                .build();
        
        user.getRoles().add(agentRole);
        userRepository.save(user);
        
        //  les tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        //  le refresh token
        saveRefreshToken(user, refreshToken);
        
        return LoginResponse.builder()
                .accessToken(accessToken)
                .email(user.getEmail())
                .role(user.getUserRoleType().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .agencyId(user.getAgencyId())
                .expiresIn(jwtService.getJwtExpiration())
                .status("ACTIVE")
                .build();
    }

    @Transactional
    public LoginResponse registerDirecteur(RegisterRequest request) {
        log.info("Creation d'un directeur d'agence: {}", request.getEmail());
        
        //  si l'utilisateur existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }
        
        //  le rôle DIRECTEUR_AGENCE
        Role directeurRole = roleRepository.findByName("ROLE_DIRECTEUR_AGENCE")
                .orElseThrow(() -> new RuntimeException("Rôle DIRECTEUR_AGENCE non trouvé"));
        
        //  le nouvel utilisateur
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(sanitizePhone(request.getPhoneNumber()))
                .userRoleType(UserRoleType.DIRECTEUR_AGENCE)
                .enabled(true)
                .build();
        
        user.getRoles().add(directeurRole);
        userRepository.save(user);
        
        //  les tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        //  le refresh token
        saveRefreshToken(user, refreshToken);
        
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .role(user.getUserRoleType().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .agencyId(user.getAgencyId())
                .expiresIn(jwtService.getJwtExpiration())
                .status("ACTIVE")
                .build();
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.debug("Tentative de connexion: {}", request.getEmail());
        
        Authentication authentication;
        
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            
            if (!authentication.isAuthenticated()) {
                throw new BadCredentialsException("Authentification échouée");
            }
            
        } catch (BadCredentialsException e) {
            log.error("Echec d'authentification pour: {}", request.getEmail());
            throw new BadCredentialsException("Bad credentials");
        }
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        //  à jour la dernière connexion dans AUTH DB
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        //  une session
        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIp(httpRequest);
        UserSession session = sessionService.createSession(user, deviceInfo, ipAddress);
        
        //  les tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        //  le refresh token
        saveRefreshToken(user, refreshToken);
       //  L'ÉVÉNEMENT DE LOGIN (ASYNCHRONE)
    
       try {
          userLoginEventPublisher.publishUserLogin(
               user.getEmail(), 
               user.getId(), 
               ipAddress, 
               deviceInfo,
               session.getSessionToken()
           );
           log.info("Evenement de login publie pour: {}", user.getEmail());
        } catch (Exception e) {
           log.warn("Erreur lors de la publication de l'evenement: {}", e.getMessage());
           //  login continue même si l'événement échoue
        }
    
        log.debug("Connexion reussie pour: {}", user.getEmail());
        
        String status = "ACTIVE";
        if (user.getUserRoleType() == UserRoleType.CLIENT) {
            try {
                JsonNode clientData = clientServiceClient.getClientByEmailPublic(user.getEmail());
                status = nodeText(clientData, "status", "ACTIVE");
            } catch (Exception e) {
                log.warn("Impossible de recuperer le status client: {}", e.getMessage());
            }
        }
    
        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .sessionToken(session.getSessionToken())
            .email(user.getEmail())
            .role(user.getUserRoleType().name())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .agencyId(user.getAgencyId())
            .expiresIn(jwtService.getJwtExpiration())
            .hasSecurityQuestion(user.isSecurityQuestionEnabled())
            .status(status)
            .build();
    } 
    
    private void saveRefreshToken(User user, String refreshToken) {
        //  l'ancien refresh token s'il existe
        refreshTokenRepository.deleteByUserId(user.getId());
        
        //  le nouveau refresh token
        RefreshToken token = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();
        
        refreshTokenRepository.save(token);
    }
    
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .userRoleType(user.getUserRoleType().name())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .lastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null)
                .build();
    }
    
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Mot de passe actuel incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Mot de passe modifié pour: {}", email);
    }

    //  pour obtenir l'IP du client
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
