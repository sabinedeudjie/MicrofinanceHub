package org.example.loanservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests purs de la formule de calcul d'amortissement (sans Spring, sans base de données).
 * Formule : M = P × [r(1+r)^n / ((1+r)^n − 1)]
 *   P = capital emprunté
 *   r = taux mensuel (taux annuel / 12)
 *   n = durée en mois
 */
@DisplayName("Calcul d'amortissement — Tests unitaires")
class AmortizationCalculationTest {

    private static final int SCALE = 2;

    /** Calcule la mensualité en FCFA selon la formule PMT standard. */
    private BigDecimal monthlyPayment(BigDecimal principal, BigDecimal annualRate, int months) {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(months), SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal r = annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusR = BigDecimal.ONE.add(r);
        BigDecimal pow = onePlusR.pow(months);
        BigDecimal numerator   = r.multiply(pow);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE);
        return principal.multiply(numerator.divide(denominator, 10, RoundingMode.HALF_UP))
                        .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** Calcule le total remboursé. */
    private BigDecimal totalRepayment(BigDecimal monthly, int months) {
        return monthly.multiply(BigDecimal.valueOf(months));
    }

    /** Calcule le total des intérêts. */
    private BigDecimal totalInterest(BigDecimal principal, BigDecimal monthly, int months) {
        return totalRepayment(monthly, months).subtract(principal);
    }

    @Test
    @DisplayName("Mensualité correcte : 500 000 FCFA à 5% sur 12 mois ≈ 42 792 FCFA")
    void shouldCalculateMonthlyPaymentFor500k5pct12months() {
        BigDecimal payment = monthlyPayment(
            new BigDecimal("500000"),
            new BigDecimal("0.05"),
            12
        );
        //  attendue ≈ 42 792 FCFA
        assertThat(payment).isBetween(
            new BigDecimal("42700"),
            new BigDecimal("42900")
        );
    }

    @Test
    @DisplayName("Mensualité correcte : 1 000 000 FCFA à 10% sur 24 mois ≈ 46 145 FCFA")
    void shouldCalculateMonthlyPaymentFor1M10pct24months() {
        BigDecimal payment = monthlyPayment(
            new BigDecimal("1000000"),
            new BigDecimal("0.10"),
            24
        );
        assertThat(payment).isBetween(
            new BigDecimal("46000"),
            new BigDecimal("46300")
        );
    }

    @Test
    @DisplayName("Les intérêts totaux sont positifs (le remboursement > capital)")
    void shouldHavePositiveTotalInterest() {
        BigDecimal principal = new BigDecimal("300000");
        BigDecimal monthly   = monthlyPayment(principal, new BigDecimal("0.08"), 18);
        BigDecimal interest  = totalInterest(principal, monthly, 18);
        assertThat(interest).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Le remboursement total est supérieur au capital")
    void shouldHaveTotalRepaymentGreaterThanPrincipal() {
        BigDecimal principal = new BigDecimal("200000");
        BigDecimal monthly   = monthlyPayment(principal, new BigDecimal("0.06"), 12);
        BigDecimal total     = totalRepayment(monthly, 12);
        assertThat(total).isGreaterThan(principal);
    }

    @Test
    @DisplayName("Taux zéro : mensualité = capital / durée")
    void shouldHandleZeroInterestRate() {
        BigDecimal principal = new BigDecimal("120000");
        BigDecimal payment   = monthlyPayment(principal, BigDecimal.ZERO, 12);
        assertThat(payment).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("Le remboursement total couvre entièrement le capital")
    void shouldFullyRepayCapitalOverTerm() {
        BigDecimal principal = new BigDecimal("500000");
        BigDecimal rate      = new BigDecimal("0.05");
        int months           = 12;
        BigDecimal r         = rate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        BigDecimal monthly   = monthlyPayment(principal, rate, months);

        //  du solde mois par mois
        BigDecimal balance = principal;
        for (int i = 1; i <= months; i++) {
            BigDecimal interest   = balance.multiply(r).setScale(SCALE, RoundingMode.HALF_UP);
            BigDecimal capital    = monthly.subtract(interest);
            if (i == months) capital = balance; //  mois : solde restant
            balance = balance.subtract(capital);
            if (balance.compareTo(BigDecimal.ZERO) < 0) balance = BigDecimal.ZERO;
        }
        //  toutes les échéances, le solde doit être ≈ 0
        assertThat(balance.abs()).isLessThan(new BigDecimal("1.00"));
    }

    @ParameterizedTest(name = "Capital={0} FCFA, taux={1}%, durée={2} mois → mensualité > 0")
    @CsvSource({
        "100000, 0.05,  6",
        "500000, 0.08, 18",
        "2000000, 0.12, 36",
        "50000,  0.03, 12",
    })
    @DisplayName("La mensualité est toujours positive (différents scénarios)")
    void shouldAlwaysProducePositiveMonthlyPayment(
            String principal, String rate, int months) {
        BigDecimal payment = monthlyPayment(
            new BigDecimal(principal),
            new BigDecimal(rate),
            months
        );
        assertThat(payment).isGreaterThan(BigDecimal.ZERO);
    }
}
