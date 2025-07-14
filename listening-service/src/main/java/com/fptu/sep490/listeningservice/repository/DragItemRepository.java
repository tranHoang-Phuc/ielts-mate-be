package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.DragItem;
import com.fptu.sep490.listeningservice.model.QuestionGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DragItemRepository extends JpaRepository<DragItem, UUID> {

    @Query("""
       SELECT di FROM DragItem di
           WHERE 
               (di.dragItemId in (SELECT i.dragItemId FROM DragItem i WHERE i.questionGroup.groupId = :groupId) AND di.isOriginal = true AND di.isCurrent = true and di.isDeleted =false)
               OR 
               (di.parent.dragItemId in (SELECT i.dragItemId FROM DragItem i WHERE i.questionGroup.groupId = :groupId) AND di.isCurrent = true and di.isDeleted =false)        
    """)
    List<DragItem> findCurrentVersionByGroupId(UUID groupId);

    @Query("""
       SELECT di FROM DragItem di
           WHERE 
               (di.dragItemId in (SELECT i.dragItemId FROM DragItem i WHERE i.questionGroup.groupId = :groupId) AND di.isOriginal = true AND di.isCurrent = true and di.isDeleted =false)
               OR 
               (di.parent.dragItemId in (SELECT i.dragItemId FROM DragItem i WHERE i.questionGroup.groupId = :groupId) AND di.isCurrent = true and di.isDeleted =false)        
    """)
    Page<DragItem> findCurrentVersionByGroupIdPaging(UUID groupId, Pageable pageable);


    @Query("""
        SELECT di FROM DragItem di
        WHERE 
            (di.dragItemId IN :dragItemIds AND di.isOriginal = true AND di.isCurrent = true and di.isDeleted =false)
            OR 
            (di.parent.dragItemId IN :dragItemIds AND di.isCurrent = true and di.isDeleted =false)
    """)
    DragItem findCurrentVersionByDragItemId(UUID dragItemId);


    Optional<DragItem> findDragItemByDragItemId(UUID dragItemId);
}
