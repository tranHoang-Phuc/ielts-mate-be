package com.fptu.sep490.personalservice.repository.specification;

import com.fptu.sep490.personalservice.model.Markup;
import com.fptu.sep490.personalservice.model.enumeration.MarkupType;
import com.fptu.sep490.personalservice.model.enumeration.PracticeType;
import com.fptu.sep490.personalservice.model.enumeration.TaskType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MarkupSpecifications {
    public static Specification<Markup> byConditions(
            List<Integer> markupTypeList,
            List<Integer> taskTypeList,
            List<Integer> practiceTypeList,
            UUID accountId
    ) {
        return ((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(markupTypeList != null && !markupTypeList.isEmpty()) {
                List<MarkupType> markupEnum = markupTypeList.stream()
                        .map(i -> MarkupType.values()[i])
                        .toList();
                predicates.add(root.get("markupType").in(markupEnum));
            }

            if(taskTypeList != null && !taskTypeList.isEmpty()) {
                List<TaskType> taskEnum = taskTypeList.stream()
                        .map(i -> TaskType.values()[i])
                        .toList();
                predicates.add(root.get("taskType").in(taskEnum));
            }

            if (practiceTypeList != null && !practiceTypeList.isEmpty()) {
                List<PracticeType> practiceEnum = practiceTypeList.stream()
                                .map(i -> PracticeType.values()[i])
                                .toList();
                predicates.add(root.get("practiceType").in(practiceEnum));
            }

            if (accountId != null) {
                predicates.add(criteriaBuilder.equal(root.get("accountId"), accountId));
            }
            query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
    }
}
