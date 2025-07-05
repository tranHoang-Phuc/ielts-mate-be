package com.fptu.sep490.listeningservice.repository;

import com.fptu.sep490.listeningservice.model.DragItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DragItemRepository extends JpaRepository<DragItem, UUID> {

    @Query("""
       SELECT di FROM DragItem di
           WHERE 
               (di.dragItemId in (SELECT i.dragItemId FROM DragItem i WHERE i.questionGroup.groupId = :groupId) AND di.isOriginal = true AND di.isCurrent = true)
               OR 
               (di.parent.dragItemId in (SELECT i.dragItemId FROM DragItem i WHERE i.questionGroup.groupId = :groupId) AND di.isCurrent = true)        
    """)
    List<DragItem> findCurrentVersionByGroupId(UUID groupId);
}
