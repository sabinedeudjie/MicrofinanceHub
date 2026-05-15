package org.example.authservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.model.Role;
import org.example.authservice.model.User;
import org.example.authservice.model.enums.UserRoleType;
import org.example.authservice.repository.RoleRepository;
import org.example.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@mfh.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin123!}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        createRolesIfAbsent();
        createDefaultAdminIfAbsent();
    }

    private void createRolesIfAbsent() {
        List<String> roleNames = List.of(
            "ROLE_ADMIN", "ROLE_AGENT", "ROLE_CLIENT", "ROLE_DIRECTEUR_AGENCE"
        );
        for (String roleName : roleNames) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder()
                    .name(roleName)
                    .description(roleName)
                    .build());
                log.info("Role cree : {}", roleName);
            }
        }
    }

    private void createDefaultAdminIfAbsent() {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Administrateur deja present : {}", adminEmail);
            return;
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
            .orElseThrow(() -> new RuntimeException("ROLE_ADMIN introuvable"));

        User admin = User.builder()
            .email(adminEmail)
            .password(passwordEncoder.encode(adminPassword))
            .firstName("Super")
            .lastName("Admin")
            .userRoleType(UserRoleType.ADMIN)
            .enabled(true)
            .build();
        admin.getRoles().add(adminRole);
        userRepository.save(admin);

        log.info("Administrateur par defaut cree : {}", adminEmail);
    }
}
