package org.example.loanservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.loanservice.model.LoanProduct;
import org.example.loanservice.repository.LoanProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final LoanProductRepository loanProductRepository;

    @Override
    public void run(String... args) {
        if (loanProductRepository.count() == 0) {
            log.info("Initialisation des produits de prêt par défaut...");

            List<LoanProduct> defaultProducts = List.of(
                LoanProduct.builder()
                    .name("Prêt Personnel")
                    .description("Prêt pour besoins personnels à court terme")
                    .minAmount(new BigDecimal("50000"))
                    .maxAmount(new BigDecimal("1000000"))
                    .minTermMonths(6)
                    .maxTermMonths(24)
                    .interestRate(new BigDecimal("12.5"))
                    .active(true)
                    .build(),
                LoanProduct.builder()
                    .name("Micro-Crédit Business")
                    .description("Financement pour petites activités commerciales")
                    .minAmount(new BigDecimal("100000"))
                    .maxAmount(new BigDecimal("5000000"))
                    .minTermMonths(12)
                    .maxTermMonths(36)
                    .interestRate(new BigDecimal("10.0"))
                    .active(true)
                    .build(),
                LoanProduct.builder()
                    .name("Prêt Scolaire")
                    .description("Soutien pour les frais de scolarité")
                    .minAmount(new BigDecimal("25000"))
                    .maxAmount(new BigDecimal("500000"))
                    .minTermMonths(3)
                    .maxTermMonths(10)
                    .interestRate(new BigDecimal("5.0"))
                    .active(true)
                    .build()
            );

            loanProductRepository.saveAll(defaultProducts);
            log.info("Produits de prêt par défaut initialisés avec succès.");
        }
    }
}
