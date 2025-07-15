package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.DragItem;
import com.fptu.sep490.listeningservice.model.QuestionGroup;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.service.DragItemService;
import com.fptu.sep490.listeningservice.viewmodel.request.DragItemCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.DragItemResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.QuestionGroupResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class DragItemServiceImpl implements DragItemService {
    ListeningTaskRepository listeningTaskRepository;
    QuestionGroupRepository questionGroupRepository;
    DragItemRepository dragItemRepository;
    QuestionRepository questionRepository;
    ChoiceRepository choiceRepository;

    Helper helper;


    @Override
    public DragItemResponse createDragItem(String groupId, DragItemCreationRequest request, HttpServletRequest httpServletRequest) {
        String userId = helper.getUserIdFromToken(httpServletRequest);

        // Validate group existence
        QuestionGroup group = questionGroupRepository.findById(UUID.fromString(groupId))
                .orElseThrow(() -> new AppException(
                                Constants.ErrorCodeMessage.NOT_FOUND,
                                Constants.ErrorCode.NOT_FOUND,
                                HttpStatus.NOT_FOUND.value()
                        )
                );

        // Create DragItem
        DragItem dragItem = DragItem.builder()
                .content(request.content())
                .createdBy(userId)
                .questionGroup(group)
                .build();
        dragItemRepository.save(dragItem);
        log.info("Drag item created with ID: {}", dragItem.getDragItemId());
        // Return response
        QuestionGroupResponse groupResponse = QuestionGroupResponse.builder()
                .groupId(UUID.fromString(group.getGroupId().toString()))
                .listeningTaskId(group.getListeningTask().getTaskId())
                .sectionOrder(group.getSectionOrder())
                .sectionLabel(group.getSectionLabel())
                .instruction(group.getInstruction())
                .createdBy(group.getCreatedBy())
                .createdAt(group.getCreatedAt())
                .updatedBy(group.getUpdatedBy())
                .updatedAt(group.getUpdatedAt())
                .isCurrent(group.getIsCurrent())
                .isDeleted(group.getIsDeleted())
                .version(group.getVersion())
                .isOriginal(group.getIsOriginal())
                .parentId(group.getParent() != null ? group.getParent().getGroupId() : null)
                .children(null)
                .dragItems(null)
                .build();
        return DragItemResponse.builder()
                .dragItemId(dragItem.getDragItemId().toString())
                .content(dragItem.getContent())
                .questionGroup(groupResponse)
                .build();
    }

    @Override
    public DragItemResponse deleteDragItem(String groupId, String itemId, HttpServletRequest httpServletRequest) {
        String userId = helper.getUserIdFromToken(httpServletRequest);

        // Validate group existence
        QuestionGroup group = questionGroupRepository.findById(UUID.fromString(groupId))
                .orElseThrow(() -> new AppException(
                                Constants.ErrorCodeMessage.NOT_FOUND,
                                Constants.ErrorCode.NOT_FOUND,
                                HttpStatus.NOT_FOUND.value()
                        )
                );

        // Validate drag item existence
        DragItem dragItem = dragItemRepository.findById(UUID.fromString(itemId))
                .orElseThrow(() -> new AppException(
                                Constants.ErrorCodeMessage.NOT_FOUND,
                                Constants.ErrorCode.NOT_FOUND,
                                HttpStatus.NOT_FOUND.value()
                        )
                );

        // Delete drag item
        dragItem.setIsDeleted(true);
        dragItemRepository.save(dragItem);
        log.info("Drag item deleted with ID: {}", dragItem.getDragItemId());


        QuestionGroupResponse groupResponse = QuestionGroupResponse.builder()
                .groupId(UUID.fromString(group.getGroupId().toString()))
                .listeningTaskId(group.getListeningTask().getTaskId())
                .sectionOrder(group.getSectionOrder())
                .sectionLabel(group.getSectionLabel())
                .instruction(group.getInstruction())
                .createdBy(group.getCreatedBy())
                .createdAt(group.getCreatedAt())
                .updatedBy(group.getUpdatedBy())
                .updatedAt(group.getUpdatedAt())
                .isCurrent(group.getIsCurrent())
                .isDeleted(group.getIsDeleted())
                .version(group.getVersion())
                .isOriginal(group.getIsOriginal())
                .parentId(group.getParent() != null ? group.getParent().getGroupId() : null)
                .children(null)
                .dragItems(null)
                .build();
        // Return response
        return DragItemResponse.builder()
                .dragItemId(dragItem.getDragItemId().toString())
                .content(dragItem.getContent())
                .questionGroup(groupResponse)
                .build();
    }

    // In DragItemServiceImpl.java

    @Override
    public DragItemResponse getDragItemByGroupIdAndItemId(String groupId, String itemId, HttpServletRequest httpServletRequest) {
        // 1. Find group
        QuestionGroup group = questionGroupRepository.findById(UUID.fromString(groupId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        QuestionGroupResponse groupResponse = QuestionGroupResponse.builder()
                .groupId(UUID.fromString(group.getGroupId().toString()))
                .listeningTaskId(group.getListeningTask().getTaskId())
                .sectionOrder(group.getSectionOrder())
                .sectionLabel(group.getSectionLabel())
                .instruction(group.getInstruction())
                .createdBy(group.getCreatedBy())
                .createdAt(group.getCreatedAt())
                .updatedBy(group.getUpdatedBy())
                .updatedAt(group.getUpdatedAt())
                .isCurrent(group.getIsCurrent())
                .isDeleted(group.getIsDeleted())
                .version(group.getVersion())
                .isOriginal(group.getIsOriginal())
                .parentId(group.getParent() != null ? group.getParent().getGroupId() : null)
                .children(null)
                .dragItems(null)
                .build();

        // 2. Find drag item
        DragItem dragItem = dragItemRepository.findById(UUID.fromString(itemId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        // 3. Check if drag item belongs to group
        if (!dragItem.getQuestionGroup().getGroupId().equals(group.getGroupId())) {
            throw new AppException(
                    "Drag item does not belong to the specified group",
                    Constants.ErrorCode.NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()
            );
        }

        // 4. Check isCurrent and isDeleted
        if (Boolean.TRUE.equals(dragItem.getIsCurrent()) && Boolean.FALSE.equals(dragItem.getIsDeleted())) {
            return DragItemResponse.builder()
                    .dragItemId(dragItem.getDragItemId().toString())
                    .content(dragItem.getContent())
                    .questionGroup(groupResponse)
                    .build();
        }

        // 5. Otherwise, search children recursively (on DragItem, not QuestionGroup)
        DragItem found = findCurrentOrChildCurrent(dragItem);
        if (found != null) {
            return DragItemResponse.builder()
                    .dragItemId(found.getDragItemId().toString())
                    .content(found.getContent())
                    .questionGroup(groupResponse)
                    .build();
        }

        throw new AppException(
                "No current drag item found in item or its children",
                Constants.ErrorCode.NOT_FOUND,
                HttpStatus.NOT_FOUND.value()
        );
    }



    @Override
    public Page<DragItemResponse> getDragItemsByGroupId(String groupId, int page, int page_size, HttpServletRequest httpServletRequest) {
        QuestionGroup group = questionGroupRepository.findById(UUID.fromString(groupId))
                .orElseThrow(() -> new AppException(
                                Constants.ErrorCodeMessage.NOT_FOUND,
                                Constants.ErrorCode.NOT_FOUND,
                                HttpStatus.NOT_FOUND.value()
                        )
                );
        Pageable pageable = PageRequest.of(page, page_size);
        Page<DragItem> dragItemsPage = dragItemRepository.findCurrentVersionByGroupIdPaging(UUID.fromString(groupId), pageable);

        return dragItemsPage.map(dragItem -> {
            QuestionGroupResponse groupResponse = QuestionGroupResponse.builder()
                    .groupId(UUID.fromString(group.getGroupId().toString()))
                    .listeningTaskId(group.getListeningTask().getTaskId())
                    .sectionOrder(group.getSectionOrder())
                    .sectionLabel(group.getSectionLabel())
                    .instruction(group.getInstruction())
                    .createdBy(group.getCreatedBy())
                    .createdAt(group.getCreatedAt())
                    .updatedBy(group.getUpdatedBy())
                    .updatedAt(group.getUpdatedAt())
                    .isCurrent(group.getIsCurrent())
                    .isDeleted(group.getIsDeleted())
                    .version(group.getVersion())
                    .isOriginal(group.getIsOriginal())
                    .parentId(group.getParent() != null ? group.getParent().getGroupId() : null)
                    .children(null)
                    .dragItems(null)
                    .build();
            return DragItemResponse.builder()
                    .dragItemId(dragItem.getDragItemId().toString())
                    .content(dragItem.getContent())
                    .questionGroup(groupResponse)
                    .build();
        });


    }

    @Override
    public DragItemResponse updateDragItem(String groupId, String itemId, DragItemCreationRequest request, HttpServletRequest httpServletRequest) throws Exception {
        String UserId = helper.getUserIdFromToken(httpServletRequest);

        DragItem dragItem = dragItemRepository.findById(UUID.fromString(itemId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        QuestionGroup group = questionGroupRepository.findById(UUID.fromString(groupId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        QuestionGroupResponse groupResponse = QuestionGroupResponse.builder()
                .groupId(UUID.fromString(group.getGroupId().toString()))
                .listeningTaskId(group.getListeningTask().getTaskId())
                .sectionOrder(group.getSectionOrder())
                .sectionLabel(group.getSectionLabel())
                .instruction(group.getInstruction())
                .createdBy(group.getCreatedBy())
                .createdAt(group.getCreatedAt())
                .updatedBy(group.getUpdatedBy())
                .updatedAt(group.getUpdatedAt())
                .isCurrent(group.getIsCurrent())
                .isDeleted(group.getIsDeleted())
                .version(group.getVersion())
                .isOriginal(group.getIsOriginal())
                .parentId(group.getParent() != null ? group.getParent().getGroupId() : null)
                .children(null)
                .dragItems(null)
                .build();
        // Check if the drag item belongs to the specified group
        if (!dragItem.getQuestionGroup().getGroupId().equals(group.getGroupId())) {
            throw new AppException(
                    "Drag item does not belong to the specified group",
                    Constants.ErrorCode.NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()
            );
        }

        DragItem currentOrChild = findCurrentOrChildCurrent(dragItem);
        if (currentOrChild == null) {
            throw new AppException(
                    "No current drag item found in item or its children",
                    Constants.ErrorCode.NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()
            );
        }

        if(currentOrChild.getIsCurrent() && dragItem.getIsDeleted() == false) {
            // Update the drag item
            DragItem newDragItem = new DragItem();
            newDragItem.setContent(request.content());
            newDragItem.setQuestionGroup(group);
            newDragItem.setIsCurrent(true);
            newDragItem.setIsDeleted(false);
            newDragItem.setVersion(currentOrChild.getVersion() + 1);
            newDragItem.setParent(dragItem);
            newDragItem.setIsOriginal(false);
            newDragItem.setUpdatedBy(UserId);
            newDragItem.setUpdatedAt(LocalDateTime.now());
            newDragItem.setQuestion(currentOrChild.getQuestion());

            currentOrChild.setIsCurrent(false);
            dragItemRepository.save(newDragItem);
            dragItemRepository.save(currentOrChild);
            log.info("Drag item updated with ID: {}", dragItem.getDragItemId());

            return DragItemResponse.builder()
                    .dragItemId(newDragItem.getDragItemId().toString())
                    .content(newDragItem.getContent())
                    .questionGroup(groupResponse)
                    .build();
        }
        return null;
    }

    // Recursive search for current drag item in children DragItems
    private DragItem findCurrentOrChildCurrent(DragItem dragItem) {
        // If no children, check if this item is current and not deleted
        if (dragItem.getChildren() == null || dragItem.getChildren().isEmpty()) {
            if (Boolean.TRUE.equals(dragItem.getIsCurrent()) && Boolean.FALSE.equals(dragItem.getIsDeleted())) {
                return dragItem;
            }
            return null;
        }
        // If has children, return the first child with isCurrent == true and isDeleted == false
        for (DragItem child : dragItem.getChildren()) {
            if (Boolean.TRUE.equals(child.getIsCurrent()) && Boolean.FALSE.equals(child.getIsDeleted())) {
                return child;
            }
        }
        return null;
    }

}
