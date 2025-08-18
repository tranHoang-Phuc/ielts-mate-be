package com.fptu.sep490.listeningservice.model.specification;

import com.fptu.sep490.listeningservice.model.ExamAttempt;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ExamAttemptSpecification {
    public static Specification<ExamAttempt> byConditions(
            String listeningExamName,
            String sortBy,
            String sortDirection,
            String createdBy
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (listeningExamName != null && !listeningExamName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("listeningExamName")), "%" + listeningExamName.toLowerCase() + "%"));
            }

            if (createdBy != null && !createdBy.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("createdBy")), "%" + createdBy.toLowerCase() + "%"));
            }
//            predicates.add(cb.isFalse(root.get("isDeleted")));
//            predicates.add(cb.isTrue(root.get("isOriginal")));

            // Add condition for history != null -> submitted exam attempts
            predicates.add(cb.isNotNull(root.get("history")));

            final String finalSortField = sortBy != null ? sortBy : "updatedAt";
            final String finalSortDirection = sortDirection != null ? sortDirection : "desc";

            // Apply sorting
            if ("asc".equalsIgnoreCase(finalSortDirection)) {
                query.orderBy(cb.asc(root.get(finalSortField)));
            } else {
                query.orderBy(cb.desc(root.get(finalSortField)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
