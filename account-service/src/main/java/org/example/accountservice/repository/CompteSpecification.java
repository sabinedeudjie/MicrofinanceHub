package org.example.accountservice.repository;

import org.example.accountservice.model.Compte;
import org.example.accountservice.model.StatutCompte;
import org.example.accountservice.model.TypeCompte;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class CompteSpecification {

    private CompteSpecification() {}

    public static Specification<Compte> withCriteria(
            String clientId,
            String numeroCompte,
            TypeCompte typeCompte,
            StatutCompte statut,
            BigDecimal soldeMin,
            BigDecimal soldeMax) {

        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (clientId != null) {
                predicates.add(cb.equal(root.get("clientId"), clientId));
            }
            if (numeroCompte != null && !numeroCompte.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("numeroCompte")),
                        "%" + numeroCompte.toLowerCase() + "%"
                ));
            }
            if (typeCompte != null) {
                predicates.add(cb.equal(root.get("typeCompte"), typeCompte));
            }
            if (statut != null) {
                predicates.add(cb.equal(root.get("statut"), statut));
            }
            if (soldeMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("solde"), soldeMin));
            }
            if (soldeMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("solde"), soldeMax));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
