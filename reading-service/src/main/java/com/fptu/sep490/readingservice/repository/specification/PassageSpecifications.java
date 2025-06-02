package com.fptu.sep490.readingservice.repository.specification;

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

public class PassageSpecifications {

    public static Specification<ReadingPassage> byConditions(
            Integer ieltsType,
            Integer status,
            Integer partNumber,
            String questionCategory
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (ieltsType != null) {
                IeltsType typeEnum = IeltsType.values()[ieltsType];
                predicates.add(cb.equal(root.get("ieltsType"), typeEnum));
            }
            if (status != null) {
                Status statusEnum = Status.values()[status];
                predicates.add(cb.equal(root.get("passageStatus"), statusEnum));
            }
            if (partNumber != null) {
                PartNumber partEnum = PartNumber.values()[partNumber];
                predicates.add(cb.equal(root.get("partNumber"), partEnum));
            }
            if (questionCategory != null && !questionCategory.isBlank()) {
                Join<ReadingPassage, QuestionGroup> joinGroup =
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
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
