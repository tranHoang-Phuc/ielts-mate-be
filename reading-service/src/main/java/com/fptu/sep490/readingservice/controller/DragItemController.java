package com.fptu.sep490.readingservice.controller;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.readingservice.service.DragItemService;
import com.fptu.sep490.readingservice.viewmodel.request.CreateDragItemRequest;
import com.fptu.sep490.readingservice.viewmodel.request.DragItemListResponse;
import com.fptu.sep490.readingservice.viewmodel.request.DragItemResponse;
import com.fptu.sep490.readingservice.viewmodel.request.UpdateDragItemRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/groups/{group-id}/items")
@Tag(name = "Drag Items", description = "API cho Drag & Drop Items dưới QuestionGroup")
public class DragItemController {

    private final DragItemService dragItemService;


    @Operation(summary = "Tạo mới một drag item")
    @PostMapping
    public ResponseEntity<BaseResponse<List<DragItemResponse>>> createDragItem(
            @PathVariable("group-id") UUID groupId,
            @Valid @RequestBody CreateDragItemRequest request,
            HttpServletRequest httpServletRequest
    ) throws AppException {
        List<DragItemResponse> response = dragItemService.createDragItem(groupId, request, httpServletRequest);
        BaseResponse<List<DragItemResponse>> body = BaseResponse.<List<DragItemResponse>>builder()
                .message("Create drag and drop item successfully")
                .data(response)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "Update a drag item")
    @PutMapping("/{item-id}")
    public ResponseEntity<BaseResponse<DragItemResponse>> updateDragItem(
            @PathVariable("group-id") UUID groupId,
            @PathVariable("item-id") UUID itemId,
            @Valid @RequestBody UpdateDragItemRequest request,
            HttpServletRequest httpServletRequest
    ) throws AppException {
        DragItemResponse response = dragItemService.updateDragItem(groupId, itemId, request, httpServletRequest);
        BaseResponse<DragItemResponse> body = BaseResponse.<DragItemResponse>builder()
                .message("Update drag and drop item successfully")
                .data(response)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }

    @Operation(summary = "Delete a drag item")
    @DeleteMapping("/{item-id}")
    public ResponseEntity<BaseResponse<Void>> deleteDragItem(
            @PathVariable("group-id") UUID groupId,
            @PathVariable("item-id") UUID itemId,
            HttpServletRequest httpServletRequest
    ) throws AppException {
        dragItemService.deleteDragItem(groupId, itemId, httpServletRequest);
        BaseResponse<Void> body = BaseResponse.<Void>builder()
                .status("success")
                .message("Delete drag and drop item successfully")
                .data(null)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }

    @Operation(summary = "Get a drag item by its ID")
    @GetMapping("/{item-id}")
    public ResponseEntity<BaseResponse<DragItemResponse>> getDragItemById(
            @PathVariable("group-id") UUID groupId,
            @PathVariable("item-id") UUID itemId
    ) throws AppException {
        DragItemResponse response = dragItemService.getDragItemById(groupId, itemId);
        BaseResponse<DragItemResponse> body = BaseResponse.<DragItemResponse>builder()
                .message(null)
                .data(response)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }


    @Operation(summary = "Get all drag items by group ID")
    @GetMapping
    public ResponseEntity<BaseResponse<DragItemListResponse>> getAllDragItemsByGroupId(
            @PathVariable("group-id") UUID groupId
    ) throws AppException {
        DragItemListResponse response = dragItemService.getAllDragItemsByGroup(groupId);
        BaseResponse<DragItemListResponse> body = BaseResponse.<DragItemListResponse>builder()
                .message(null)
                .data(response)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }


}
