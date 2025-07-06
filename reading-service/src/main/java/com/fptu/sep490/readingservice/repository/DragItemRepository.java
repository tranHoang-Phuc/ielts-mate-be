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
        WHERE di.question.questionId = :questionId
    """)
    DragItem findByQuestionId(UUID questionId);
    @Query("""
        SELECT di FROM DragItem di 
        WHERE (di.dragItemId = :originalDragItemId AND di.isCurrent = true)
           OR (di.parent.dragItemId = :originalDragItemId AND di.isCurrent = true)
    """)
    List<DragItem> findPreviousDragItems(UUID originalDragItemId);

    @Query("""
    SELECT di FROM DragItem di
    WHERE di.isCurrent = true AND (
        di.dragItemId IN (
            SELECT original.dragItemId FROM DragItem original
            WHERE original.questionGroup.groupId = :groupId AND original.isOriginal = true
        )
        OR di.parent.dragItemId IN (
            SELECT original.dragItemId FROM DragItem original
            WHERE original.questionGroup.groupId = :groupId AND original.isOriginal = true
        )
    )
    """)
    List<DragItem> findCurrentVersionsByGroupId(UUID groupId);

    @Query("""
       SELECT di FROM DragItem di
           WHERE 
               (di.dragItemId in (SELECT i.dragItemId FROM DragItem i WHERE i.questionGroup.groupId = :groupId) AND di.isOriginal = true AND di.isCurrent = true and di.isDeleted =false)
               OR 
               (di.parent.dragItemId in (SELECT i.dragItemId FROM DragItem i WHERE i.questionGroup.groupId = :groupId) AND di.isCurrent = true and di.isDeleted =false)        
    """)
    List<DragItem> findCurrentVersionByGroupId(UUID groupId);
}
