package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.DragItem;
import org.springframework.data.jpa.repository.JpaRepository;
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

    /**
     * Lấy tất cả DragItem theo group_id (order by drag_item_id để cố định thứ tự).
     */
//    List<DragItem> findAllByGroupIdOrderById(UUID groupId);
//
//    /**
//     * Tìm một DragItem theo drag_item_id & group_id.
//     */
//    Optional<DragItem> findByIdAndGroupId(UUID id, UUID groupId);
//
//    /**
//     * Xóa DragItem theo id & group_id.
//     */
//    void deleteByIdAndGroupId(UUID id, UUID groupId);

    Optional<DragItem> findByDragItemIdAndQuestionGroup_GroupId(UUID dragItemId, UUID groupId);
    List<DragItem> findAllByQuestionGroup_GroupId(UUID groupId);
}
