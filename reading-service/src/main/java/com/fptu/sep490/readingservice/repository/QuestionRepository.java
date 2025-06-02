package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho bảng questions.
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

//    /**
//     * Lấy tất cả Question theo question_group_id, order by question_order.
//     */
//    List<Question> findAllByQuestionGroupIdOrderByQuestionOrder(UUID groupId);
//
//    /**
//     * Tìm Question theo question_id & question_group_id.
//     */
//    Optional<Question> findByQuestionIdAndQuestionGroupId(UUID questionId, UUID groupId);

//    Optional<Question> findByQuestionId(UUID questionId);
}
