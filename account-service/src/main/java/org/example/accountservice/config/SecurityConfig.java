package org.example.accountservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration de la sécurité Spring Security.
 *
 * Règles d'accès :
 *  - /actuator/health       → accessible sans token (pour les health checks Docker/Kubernetes)
 *  - GET /api/comptes/**    → CLIENT peut voir ses propres comptes
 *  - POST /api/comptes      → AGENT ou ADMIN peut ouvrir un compte
 *  - POST /api/comptes/*2/depot, /retrait → CLIENT, AGENT, ADMIN
 *  - DELETE /api/comptes/** → ADMIN uniquement
 *  - Tout le reste          → authentifié
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   //  @PreAuthorize sur les méthodes
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                //  CSRF (inutile pour une API REST stateless)
                .csrf(AbstractHttpConfigurer::disable)

                //  CORS avec notre configuration personnalisée (voir corsConfigurationSource())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                //  de session HTTP (JWT est stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                //  d'autorisation par endpoint
                .authorizeHttpRequests(auth -> auth

                        //  check ouvert (pour Docker, Kubernetes, API Gateway)
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        //  appels internes service-à-service (pas de JWT utilisateur)
                        .requestMatchers("/api/accounts/internal/**").permitAll()

                        //  UI — accessible sans JWT pour pouvoir tester
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()

                        //  de génération de token de test (profil dev uniquement)
                        .requestMatchers("/api/test/token").permitAll()

                        //  un compte : AGENT ou ADMIN seulement
                        .requestMatchers(HttpMethod.POST, "/api/comptes").hasAnyRole("AGENT", "ADMIN")

                        // valider/rejeter un compte : DIRECTEUR_AGENCE ou ADMIN (plus l'agent)
                        .requestMatchers(HttpMethod.PATCH, "/api/comptes/*/statut").hasAnyRole("ADMIN", "DIRECTEUR_AGENCE")

                        //  internes appelés par loan-service / transaction-service
                        .requestMatchers("/api/internal/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/comptes/*/crediter").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/comptes/*/debiter").hasRole("ADMIN")

                        //  : tous les utilisateurs authentifiés
                        .requestMatchers(HttpMethod.GET, "/api/comptes/**").authenticated()

                        //  : ADMIN uniquement
                        .requestMatchers(HttpMethod.DELETE, "/api/comptes/**").hasRole("ADMIN")

                        //  défaut : toute requête doit être authentifiée
                        .anyRequest().authenticated()
                )

                //  le filtre JWT AVANT le filtre d'authentification standard
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuration CORS — définit quelles origines peuvent appeler l'API.
     *
     * En développement : autorise uniquement localhost:3000 (React dev server).
     * En production : remplacer par l'URL réelle du frontend.
     *
     * Règles appliquées :
     *   - Origins  : http://
     *   - Methods  : GET, POST, PUT, PATCH, DELETE, OPTIONS
     *   - Headers  : Authorization (pour le token JWT), Content-Type
     *   - Credentials : true (nécessaire pour envoyer le header Authorization)
     *   - MaxAge   : 3600s → le navigateur met en cache la réponse preflight 1h
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        //  autorisée : le frontend React en développement local
        config.setAllowedOriginPatterns(List.of("*"));

        //  HTTP autorisées
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        //  que le frontend peut envoyer
        // "" est indispensable pour transmettre le JWT Bearer token
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        //  l'envoi des credentials (headers Authorization notamment)
        config.setAllowCredentials(true);

        //  de mise en cache du preflight CORS côté navigateur (en secondes)
        config.setMaxAge(3600L);

        //  cette configuration à tous les endpoints de l'API
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}