package com.fptu.sep490.listeningservice.controller;


import com.fptu.sep490.commonlibrary.constants.PageableConstant;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.commonlibrary.viewmodel.response.Pagination;
import com.fptu.sep490.listeningservice.service.DragItemService;
import com.fptu.sep490.listeningservice.service.GroupService;
import com.fptu.sep490.listeningservice.viewmodel.request.DragItemCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.request.ListeningTaskCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.request.QuestionGroupCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.DragItemResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.QuestionGroupResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/groups")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class DragItemController {
    DragItemService dragItemService;

    @PostMapping(value = "/{group-id}/items", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<DragItemResponse>> createDragItem(
            @PathVariable("group-id") String groupId,
            @Valid @org.springframework.web.bind.annotation.RequestBody DragItemCreationRequest request,
            HttpServletRequest httpServletRequest) throws Exception {
        DragItemResponse response = dragItemService.createDragItem(groupId, request, httpServletRequest);
        BaseResponse<DragItemResponse> baseResponse = BaseResponse.<DragItemResponse>builder()
                .data(response)
                .message("Drag item created successfully")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    @DeleteMapping("/{group-id}/items/{item-id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<DragItemResponse>> deleteDragItem(
            @PathVariable("group-id") String groupId,
            @PathVariable("item-id") String itemId,
            HttpServletRequest httpServletRequest) throws Exception {
        DragItemResponse response = dragItemService.deleteDragItem(groupId, itemId, httpServletRequest);
        BaseResponse<DragItemResponse> baseResponse = BaseResponse.<DragItemResponse>builder()
                .data(response)
                .message("Drag item deleted successfully")
                .build();
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    @GetMapping("/{group-id}/items")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<List<DragItemResponse>>> getDragItems(
            @PathVariable("group-id") String groupId,
            @RequestParam(value = "page", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", required = false, defaultValue = PageableConstant.DEFAULT_PAGE_SIZE) int size,
            HttpServletRequest httpServletRequest) throws Exception {

        Page<DragItemResponse> pageableItems = dragItemService.getDragItemsByGroupId(groupId, page - 1, size, httpServletRequest);

        Pagination pagination = Pagination.builder()
                .currentPage(pageableItems.getNumber() + 1)
                .totalPages(pageableItems.getTotalPages())
                .pageSize(pageableItems.getSize())
                .totalItems((int) pageableItems.getTotalElements())
                .hasNextPage(pageableItems.hasNext())
                .hasPreviousPage(pageableItems.hasPrevious())
                .build();

        BaseResponse<List<DragItemResponse>> baseResponse = BaseResponse.<List<DragItemResponse>>builder()
                .data(pageableItems.getContent())
                .pagination(pagination)
                .message("Drag items retrieved successfully")
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }

    @GetMapping("/{group-id}/items/{item-id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<BaseResponse<DragItemResponse>> getDragItem(
            @PathVariable("group-id") String groupId,
            @PathVariable("item-id") String itemId,
            HttpServletRequest httpServletRequest) throws Exception {
        // Assuming a method exists in DragItemService to fetch a drag item by group ID and item ID
        DragItemResponse response = dragItemService.getDragItemByGroupIdAndItemId(groupId, itemId, httpServletRequest);
        BaseResponse<DragItemResponse> baseResponse = BaseResponse.<DragItemResponse>builder()
                .data(response)
                .message("Drag item retrieved successfully")
                .build();
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }
}
