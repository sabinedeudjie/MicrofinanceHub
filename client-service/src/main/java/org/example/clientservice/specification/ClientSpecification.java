package org.example.clientservice.specification;

import org.example.clientservice.model.Client;
import org.example.clientservice.model.enums.ClientStatus;
import org.example.clientservice.model.enums.ClientType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ClientSpecification {
    
    public static Specification<Client> searchWithFilters(
            String email,
            String phone,
            ClientStatus status,
            ClientType clientType,
            Integer minScore,
            Integer maxScore,
            LocalDateTime createdAfter,
            String searchTerm) {
        
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            //  email
            if (StringUtils.hasText(email)) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            }
            
            //  téléphone
            if (StringUtils.hasText(phone)) {
                predicates.add(cb.like(root.get("phoneNumber"), "%" + phone + "%"));
            }
            
            //  statut
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            //  type de client
            if (clientType != null) {
                predicates.add(cb.equal(root.get("clientType"), clientType));
            }
            
            //  score minimum
            if (minScore != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("creditScore"), minScore));
            }
            
            //  score maximum
            if (maxScore != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("creditScore"), maxScore));
            }
            
            //  date de création
            if (createdAfter != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter));
            }
            
            //  globale
            if (StringUtils.hasText(searchTerm)) {
                String pattern = "%" + searchTerm.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
                ));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}