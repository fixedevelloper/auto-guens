package com.ussdauto.api.repository;

import com.ussdauto.api.domain.entity.Transaction;
import com.ussdauto.api.web.dto.TransactionFilterParams;
import org.springframework.data.jpa.domain.Specification;

public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> fromFilters(TransactionFilterParams filters) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (filters.operationType() != null) {
                predicates = cb.and(predicates, cb.equal(root.get("operationType"), filters.operationType()));
            }
            if (filters.operator() != null) {
                predicates = cb.and(predicates, cb.equal(root.get("operator"), filters.operator()));
            }
            if (filters.statut() != null) {
                predicates = cb.and(predicates, cb.equal(root.get("statut"), filters.statut()));
            }
            if (filters.from() != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("createdAt"), filters.from()));
            }
            if (filters.to() != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("createdAt"), filters.to()));
            }
            return predicates;
        };
    }
}
