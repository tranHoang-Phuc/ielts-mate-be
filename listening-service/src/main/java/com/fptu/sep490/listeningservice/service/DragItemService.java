package com.fptu.sep490.listeningservice.service;

import com.fptu.sep490.listeningservice.viewmodel.request.DragItemCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.DragItemResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;

public interface DragItemService {

    DragItemResponse createDragItem(String groupId, DragItemCreationRequest request, HttpServletRequest httpServletRequest) throws Exception;

    DragItemResponse deleteDragItem(String groupId, String itemId, HttpServletRequest httpServletRequest) throws Exception;


    DragItemResponse getDragItemByGroupIdAndItemId(String groupId, String itemId, HttpServletRequest httpServletRequest) throws Exception;

    Page<DragItemResponse> getDragItemsByGroupId(String groupId, int i, int size, HttpServletRequest httpServletRequest);

    DragItemResponse updateDragItem(String groupId, String itemId, DragItemCreationRequest request, HttpServletRequest httpServletRequest) throws Exception;
}
