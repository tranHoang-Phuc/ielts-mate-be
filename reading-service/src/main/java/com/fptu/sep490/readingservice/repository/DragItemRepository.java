package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.DragItem;
import com.fptu.sep490.readingservice.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho bảng drag_items.
 */
@Repository
public interface DragItemRepository extends JpaRepository<DragItem, UUID> {
    Optional<DragItem> findDragItemByDragItemId(UUID dragItemId);

    Optional<DragItem> findByDragItemIdAndQuestionGroup_GroupIdAndIsDeleted(UUID dragItemId, UUID questionGroupGroupId, Boolean isDeleted);
    List<DragItem> findAllByQuestionGroup_GroupId(UUID groupId);

    Optional<DragItem> findByQuestion(Question question);

    @Query("""
        SELECT di FROM DragItem di 
        WHERE (di.dragItemId = :originalDragItemId AND di.isCurrent = true)
           OR (di.parent.dragItemId = :originalDragItemId AND di.isCurrent = true)
    """)
    List<DragItem> findPreviousDragItems(UUID originalDragItemId);

}
