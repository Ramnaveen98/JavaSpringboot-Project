package com.autobridge_api.requests;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;

public final class RequestSpecs {
    private RequestSpecs() {}

    public static Specification<ServiceRequest> byFilters(
            RequestStatus status,
            Long agentId,
            Instant from,   // createdAt >= from (inclusive)
            Instant to      // createdAt  < to   (exclusive)
    ) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (agentId != null) {
                // LEFT join because many rows will have no agent
                var agentJoin = root.join("assignedAgent", JoinType.LEFT);
                predicates.add(cb.equal(agentJoin.get("id"), agentId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), to));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
