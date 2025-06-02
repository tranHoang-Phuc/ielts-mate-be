package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.DragItem;
import com.fptu.sep490.readingservice.model.QuestionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionGroupRepository extends JpaRepository<QuestionGroup, UUID> {
    /**
     * Tìm một QuestionGroup theo id.
     *
     * @param groupId ID của QuestionGroup cần tìm.
     * @return QuestionGroup nếu tìm thấy, hoặc Optional rỗng nếu không tìm thấy.
     */
//    QuestionGroup findById(UUID id);
//    Optional<QuestionGroup> findByGroupId(UUID groupId);
}

