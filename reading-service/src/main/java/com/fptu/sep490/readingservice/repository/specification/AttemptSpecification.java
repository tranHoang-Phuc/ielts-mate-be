package com.fptu.sep490.readingservice.repository.specification;

import com.fptu.sep490.readingservice.model.Attempt;
import com.fptu.sep490.readingservice.model.ReadingPassage;
import com.fptu.sep490.readingservice.model.enumeration.IeltsType;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public  class AttemptSpecification {
    public static Specification<Attempt> byConditions(
            List<Integer> ieltsType,
            List<Integer> statusList,
            List<Integer> partNumber,
            String sortBy,
            String sortDirection,
            String title,
            UUID passageId,
            String createdBy
    ) {
        return ((root, query, criteriaBuilder) -> {
            Join<Attempt, ReadingPassage> task = root.join("readingPassage", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();
            if(passageId != null) {
                predicates.add(criteriaBuilder.equal(task.get("passageId"), passageId));
            }

            if(ieltsType != null && !ieltsType.isEmpty()) {
                List<IeltsType> typeEnums = ieltsType.stream()
                        .map(i -> IeltsType.values()[i])
                        .toList();
                predicates.add(task.get("ieltsType").in(typeEnums));
            }

            if (statusList != null && !statusList.isEmpty()) {
                predicates.add(root.get("status").in(statusList));
            }

            if (partNumber != null && !partNumber.isEmpty()) {
                predicates.add(task.get("partNumber").in(partNumber));
            }

            if (title != null && !title.isBlank()) {
                predicates.add(criteriaBuilder.like(task.get("title"), "%" + title + "%"));
            }

            if (createdBy != null && !createdBy.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("createdBy"), createdBy));
            }

            query.where(predicates.toArray(new Predicate[0]));
            if (sortBy != null && !sortBy.isBlank()) {
                Path<?> sortPath;
                if (sortBy.contains(".")) {
                    String[] parts = sortBy.split("\\.");
                    sortPath = root.get(parts[0]).get(parts[1]);
                } else {
                    sortPath = root.get(sortBy);
                }
                Order order = "desc".equalsIgnoreCase(sortDirection)
                        ? criteriaBuilder.desc(sortPath)
                        : criteriaBuilder.asc(sortPath);
                query.orderBy(order);
            }

            return query.getRestriction();
        });
    }
}
