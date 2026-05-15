package org.example.transactionservice.repository;

import jakarta.persistence.criteria.Predicate;
import org.example.transactionservice.model.StatutTransaction;
import org.example.transactionservice.model.Transaction;
import org.example.transactionservice.model.TypeTransaction;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class TransactionSpecification {

    private TransactionSpecification() {}

    public static Specification<Transaction> withCriteria(
            Long compteId,
            String reference,
            TypeTransaction typeTransaction,
            StatutTransaction statut,
            BigDecimal montantMin,
            BigDecimal montantMax,
            LocalDateTime debut,
            LocalDateTime fin) {

        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            if (compteId != null) {
                predicates.add(cb.equal(root.get("compteId"), compteId));
            }
            if (reference != null && !reference.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("reference")),
                        "%" + reference.toLowerCase() + "%"
                ));
            }
            if (typeTransaction != null) {
                predicates.add(cb.equal(root.get("typeTransaction"), typeTransaction));
            }
            if (statut != null) {
                predicates.add(cb.equal(root.get("statut"), statut));
            }
            if (montantMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("montant"), montantMin));
            }
            if (montantMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("montant"), montantMax));
            }
            if (debut != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateTransaction"), debut));
            }
            if (fin != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateTransaction"), fin));
            }

            query.orderBy(cb.desc(root.get("dateTransaction")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
