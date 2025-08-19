package com.fptu.sep490.readingservice.repository.specification;

import com.fptu.sep490.readingservice.model.ExamAttempt;
import com.fptu.sep490.readingservice.model.Question;
import com.fptu.sep490.readingservice.model.QuestionGroup;
import com.fptu.sep490.readingservice.model.ReadingPassage;
import com.fptu.sep490.readingservice.model.enumeration.IeltsType;
import com.fptu.sep490.readingservice.model.enumeration.PartNumber;
import com.fptu.sep490.readingservice.model.enumeration.QuestionCategory;
import com.fptu.sep490.readingservice.model.enumeration.Status;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ExamAttemptSpecifications {

    public static Specification<ExamAttempt> byConditions(
            String readingExamName,
            String sortBy,
            String sortDirection,
            String createdBy
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (readingExamName != null && !readingExamName.isBlank()) {
                Join<Object, Object> readingExamJoin = root.join("readingExam", JoinType.INNER);
                predicates.add(cb.like(cb.lower(readingExamJoin.get("examName")), "%" + readingExamName.toLowerCase() + "%"));
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
