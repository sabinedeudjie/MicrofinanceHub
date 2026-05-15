package org.example.repaymentservice.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class DatabaseFixConfig {

    @Bean
    public CommandLineRunner dropStatusConstraint(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                log.info("Tentative de suppression de la contrainte payments_status_check...");
                jdbcTemplate.execute("ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check");
                log.info("Contrainte payments_status_check supprimée ou inexistante.");
            } catch (Exception e) {
                log.warn("Erreur lors de la suppression de la contrainte: " + e.getMessage());
            }
        };
    }
}
