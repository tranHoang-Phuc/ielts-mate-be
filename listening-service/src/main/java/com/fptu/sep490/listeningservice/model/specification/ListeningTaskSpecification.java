package com.fptu.sep490.listeningservice.model.specification;

import com.fptu.sep490.listeningservice.model.ListeningTask;
import com.fptu.sep490.listeningservice.model.Question;
import com.fptu.sep490.listeningservice.model.QuestionGroup;
import com.fptu.sep490.listeningservice.model.enumeration.IeltsType;
import com.fptu.sep490.listeningservice.model.enumeration.PartNumber;
import com.fptu.sep490.listeningservice.model.enumeration.QuestionCategory;
import com.fptu.sep490.listeningservice.model.enumeration.Status;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ListeningTaskSpecification {
    public static Specification<ListeningTask> byCondition(
            List<Integer> ieltsType,
            List<Integer> status,
            List<Integer> partNumber,
            String questionCategory,
            String sortBy,
            String sortDirection,
            String title,
            String createdBy
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();


            if (ieltsType != null && !ieltsType.isEmpty()) {
                List<IeltsType> typeEnums = ieltsType.stream()
                        .map(i -> IeltsType.values()[i])
                        .toList();
                predicates.add(root.get("ieltsType").in(typeEnums));
            }

            if (status != null && !status.isEmpty()) {
                List<Status> statusEnums = status.stream()
                        .map(i -> Status.values()[i])
                        .toList();
                predicates.add(root.get("status").in(statusEnums));
            }

            if (partNumber != null && !partNumber.isEmpty()) {
                List<PartNumber> partEnums = partNumber.stream()
                        .map(i -> PartNumber.values()[i])
                        .toList();
                predicates.add(root.get("partNumber").in(partEnums));
            }
            if (questionCategory != null && !questionCategory.isBlank()) {
                Join<ListeningTask, QuestionGroup> joinGroup =
                        root.join("questionGroups", JoinType.LEFT);
                Join<QuestionGroup, Question> joinQuestion =
                        joinGroup.join("questions", JoinType.LEFT);
                Join<Question, QuestionCategory> joinCategory =
                        joinQuestion.joinSet("categories", JoinType.LEFT);
                QuestionCategory enumCategory;
                try {
                    enumCategory = QuestionCategory.valueOf(questionCategory.trim());
                } catch (IllegalArgumentException ex) {
                    return cb.disjunction();
                }
                Predicate categoryPredicate = cb.equal(joinCategory, enumCategory);
                predicates.add(categoryPredicate);

                query.distinct(true);
            }

            if (title != null && !title.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
            }

            if (createdBy != null && !createdBy.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("createdBy")), "%" + createdBy.toLowerCase() + "%"));
            }
            predicates.add(cb.isFalse(root.get("isDeleted")));
            predicates.add(cb.isTrue(root.get("isOriginal")));

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
