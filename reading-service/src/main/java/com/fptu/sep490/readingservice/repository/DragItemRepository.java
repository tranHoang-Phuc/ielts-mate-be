package com.fptu.sep490.readingservice.repository;

import com.fptu.sep490.readingservice.model.DragItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.ScopedValue;
import java.util.UUID;

public interface DragItemRepository extends JpaRepository<DragItem, UUID> {
    <T> ScopedValue<T> findDragItemByDragItemId(UUID dragItemId);
}
