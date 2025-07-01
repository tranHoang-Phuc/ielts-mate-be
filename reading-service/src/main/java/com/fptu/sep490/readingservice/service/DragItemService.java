package com.fptu.sep490.readingservice.service;


import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.readingservice.viewmodel.request.CreateDragItemRequest;
import com.fptu.sep490.readingservice.viewmodel.request.DragItemListResponse;
import com.fptu.sep490.readingservice.viewmodel.request.DragItemResponse;
import com.fptu.sep490.readingservice.viewmodel.request.UpdateDragItemRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.UUID;

/**
 * Chứa các business logic thao tác DragItem.
 */
public interface DragItemService {

    List<DragItemResponse> createDragItem(UUID groupId, CreateDragItemRequest request, HttpServletRequest httpServletRequest)
            throws AppException;

    DragItemResponse updateDragItem(UUID groupId, UUID itemId, UpdateDragItemRequest request,
                                    HttpServletRequest httpServletRequest) throws AppException;

    void deleteDragItem(UUID groupId,
                        UUID itemId, HttpServletRequest httpServletRequest) throws AppException;

    DragItemResponse getDragItemById(UUID groupId, UUID itemId) throws AppException;
    DragItemListResponse getAllDragItemsByGroup(UUID groupId) throws AppException;
}
